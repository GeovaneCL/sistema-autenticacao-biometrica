package biometria; // <-- Alterado aqui para refletir a nova pasta simplificada

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

public class PreProcessamentoBiometrico {

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

    // Método principal para testes
    public static void main(String[] args) {
        PreProcessamentoBiometrico bp = new PreProcessamentoBiometrico();
        
        // Substitua pelo caminho de uma imagem de teste no seu computador (ex: "teste.jpg")
        String caminho = "foto.jpg";
        
        Mat imgOriginal = bp.carregarImagem(caminho);
        if (imgOriginal != null) {
            Mat imgFinal = bp.processar(imgOriginal);
            
            // Salvando o resultado processado para validação
            opencv_imgcodecs.imwrite("biometria_processada.jpg", imgFinal);
            System.out.println("Imagem salva como 'biometria_processada.jpg'");
        }
    }
}