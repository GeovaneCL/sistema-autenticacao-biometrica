package biometria;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import biometria.BiometriaException.Tipo;

public class PreProcessamentoBiometrico {

    // Arquivo do detector facial (colocar na raiz do projeto, ao lado do pom.xml)
    public static final String CAMINHO_CASCADE = "haarcascade_frontalface_default.xml";

    // Se o arquivo não existir, é baixado uma única vez desta versão (fixa) do
    // repositório do OpenCV
    private static final String URL_CASCADE = "https://raw.githubusercontent.com/opencv/opencv/4.9.0/data/haarcascades/haarcascade_frontalface_default.xml";

    // Pasta onde ficam os rostos cadastrados (arquivos id_n.png)
    public static final String PASTA_ROSTOS = "faces";

    private static final int QUADROS_AQUECIMENTO = 15; // câmera de notebook precisa ajustar a exposição
    private static final long INTERVALO_MS = 400; // tempo mínimo entre dois rostos capturados

    // 1. Rotina de Captura (Leitura de arquivo de imagem)
    public Mat carregarImagem(String caminhoArquivo) {
        Mat imagem = opencv_imgcodecs.imread(caminhoArquivo);
        if (imagem.empty()) {
            System.err.println("Erro: Não foi possível carregar a imagem do caminho: " + caminhoArquivo);
            return null;
        }
        System.out.println("Imagem carregada com sucesso!");
        return imagem;
    }

    // 2. Pré-processamento (Cinza, Redimensionamento e Contraste/Nitidez)
    public Mat processar(Mat imagemOriginal) {
        Mat imagemProcessada = new Mat();

        // Passo A: Conversão para Escala de Cinza
        opencv_imgproc.cvtColor(imagemOriginal, imagemProcessada, opencv_imgproc.COLOR_BGR2GRAY);

        // Passo B: Redimensionamento (Padronizando para 256x256 pixels)
        Size tamanhoPadrao = new Size(256, 256);
        opencv_imgproc.resize(imagemProcessada, imagemProcessada, tamanhoPadrao);

        // Passo C: Suavização / Redução de Ruído (Filtro Gaussiano)
        opencv_imgproc.GaussianBlur(imagemProcessada, imagemProcessada, new Size(3, 3), 0);

        // Passo D: Ajuste de Contraste (Equalização de Histograma)
        opencv_imgproc.equalizeHist(imagemProcessada, imagemProcessada);

        System.out.println("Pré-processamento concluído com sucesso!");
        return imagemProcessada;
    }

    // 3. Captura de rostos pela câmera do notebook.
    // Devolve rostos já recortados e pré-processados (cinza, 256x256, filtro
    // gaussiano, contraste).
    // 'aoVivo' recebe cada quadro para a tela mostrar a prévia (pode ser null).
    public List<Mat> capturarRostos(int quantidade, Consumer<Mat> aoVivo) throws BiometriaException {
        garantirDetector();
        CascadeClassifier detector = new CascadeClassifier(CAMINHO_CASCADE);
        if (detector.empty()) {
            detector.close();
            throw new BiometriaException(Tipo.FACIAL_INDISPONIVEL,
                    "Arquivo '" + CAMINHO_CASCADE + "' não encontrado na pasta do projeto.");
        }

        VideoCapture camera = abrirCamera();
        List<Mat> rostos = new ArrayList<>();
        Mat quadro = new Mat();
        Mat cinza = new Mat();
        int quadrosLidos = 0;
        int falhasLeitura = 0;
        long ultimaCaptura = 0;
        long limite = System.currentTimeMillis() + Math.max(8000L, quantidade * 2500L);

        try {
            while (rostos.size() < quantidade && System.currentTimeMillis() < limite) {
                if (!camera.read(quadro) || quadro.empty()) {
                    if (++falhasLeitura > 50) {
                        throw new BiometriaException(Tipo.FACIAL_INDISPONIVEL,
                                "Não foi possível ler imagens da câmera.");
                    }
                    continue;
                }
                falhasLeitura = 0;
                quadrosLidos++;

                Mat previa = quadro.clone(); // cópia para desenhar o retângulo sem estragar a captura

                if (quadrosLidos > QUADROS_AQUECIMENTO) {
                    // Detecção feita em cinza com contraste equalizado (ajuda com pouca luz)
                    opencv_imgproc.cvtColor(quadro, cinza, opencv_imgproc.COLOR_BGR2GRAY);
                    opencv_imgproc.equalizeHist(cinza, cinza);

                    RectVector encontrados = new RectVector();
                    detector.detectMultiScale(cinza, encontrados, 1.1, 5, 0,
                            new Size(80, 80), new Size());
                    Rect maior = maiorRosto(encontrados);

                    if (maior != null) {
                        opencv_imgproc.rectangle(previa, maior, Scalar.GREEN);
                        if (System.currentTimeMillis() - ultimaCaptura >= INTERVALO_MS) {
                            rostos.add(processar(new Mat(quadro, maior)));
                            ultimaCaptura = System.currentTimeMillis();
                        }
                    }
                }

                if (aoVivo != null) {
                    aoVivo.accept(previa);
                }
            }
        } finally {
            camera.release();
            camera.close();
            detector.close(); // <-- novo
            quadro.close(); // <-- novo
            cinza.close();
        }

        if (rostos.isEmpty()) {
            throw new BiometriaException(Tipo.LEITURA_INVALIDA,
                    "Nenhum rosto detectado. Aproxime-se da câmera e melhore a iluminação.");
        }
        return rostos;
    }

    // Baixa o detector facial automaticamente na primeira vez (precisa de internet
    // só nessa vez)
    private void garantirDetector() throws BiometriaException {
        File arquivo = new File(CAMINHO_CASCADE);
        if (arquivo.exists()) {
            return;
        }

        try (InputStream entrada = URI.create(URL_CASCADE).toURL().openStream()) {
            Files.copy(entrada, arquivo.toPath());
            System.out.println("Detector facial baixado para: " + arquivo.getAbsolutePath());
        } catch (IOException e) {
            throw new BiometriaException(Tipo.FACIAL_INDISPONIVEL,
                    "Não foi possível baixar o detector facial (sem internet?). "
                            + "Baixe '" + CAMINHO_CASCADE + "' manualmente e coloque na raiz do projeto.",
                    e);
        }
    }

    private VideoCapture abrirCamera() throws BiometriaException {
        // No Windows, o modo DirectShow costuma abrir a câmera do notebook com mais
        // confiabilidade
        VideoCapture camera = new VideoCapture(0, opencv_videoio.CAP_DSHOW);
        if (!camera.isOpened()) {
            camera.release();
            camera = new VideoCapture(0);
        }
        if (!camera.isOpened()) {
            camera.release();
            throw new BiometriaException(Tipo.FACIAL_INDISPONIVEL,
                    "Câmera não encontrada ou em uso por outro programa.");
        }
        camera.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, 640);
        camera.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, 480);
        return camera;
    }

    // Escolhe o maior rosto do quadro (devolve uma cópia do retângulo)
    private Rect maiorRosto(RectVector rostos) {
        Rect maior = null;
        for (long i = 0; i < rostos.size(); i++) {
            Rect atual = rostos.get(i);
            if (maior == null || atual.area() > maior.area()) {
                maior = new Rect(atual.x(), atual.y(), atual.width(), atual.height());
            }
        }
        return maior;
    }

    // 4. Cadastro: salva rostos de um usuário na pasta 'faces' (arquivos id_n.png)
    public static void cadastrarRosto(int idUsuario, int quantidade) throws BiometriaException {
        new File(PASTA_ROSTOS).mkdirs();

        CanvasFrame janela = new CanvasFrame("Cadastro de rosto - usuário " + idUsuario);

        try (OpenCVFrameConverter.ToMat conversor = new OpenCVFrameConverter.ToMat()) {
            List<Mat> rostos = new PreProcessamentoBiometrico().capturarRostos(quantidade,
                    quadro -> janela.showImage(conversor.convert(quadro)));

            for (int i = 0; i < rostos.size(); i++) {
                String caminho = PASTA_ROSTOS + "/" + idUsuario + "_" + (i + 1) + ".png";
                opencv_imgcodecs.imwrite(caminho, rostos.get(i));
            }
            System.out.println(rostos.size() + " rosto(s) salvo(s) em '" + PASTA_ROSTOS + "'.");
        } finally {
            janela.dispose();
        }
    }

    // Teste antigo com arquivo de imagem (mantido)
    public static void testarComArquivo(String caminho) {
        PreProcessamentoBiometrico bp = new PreProcessamentoBiometrico();

        Mat imgOriginal = bp.carregarImagem(caminho);
        if (imgOriginal != null) {
            Mat imgFinal = bp.processar(imgOriginal);

            // Salvando o resultado processado para validação
            opencv_imgcodecs.imwrite("biometria_processada.jpg", imgFinal);
            System.out.println("Imagem salva como 'biometria_processada.jpg'");
        }
    }

    // Executar esta classe cadastra o rosto de um usuário (pede o id_usuario do
    // banco)
    public static void main(String[] args) {
        String texto = JOptionPane.showInputDialog(null,
                "ID do usuário para cadastrar o rosto:");//de acordo com o banco de dados os ID são (1,2 ou 3)
        if (texto == null) {
            System.exit(0);
        }

        try {
            cadastrarRosto(Integer.parseInt(texto.trim()), 10);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "ID inválido.");
        } catch (BiometriaException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(0);
    }
}
