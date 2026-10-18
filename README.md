/GEO/
Gente, além da diminuição das classes eu adicionei novas tabelas que é possível ver de acordo com o nível de acesso após login.Para o login via biometrica facial é necessario rodar a classe `PreProcessamentoBiometrico.java` Durante a execução vai pedir primeiro o dado de acordo que está na tabela (SELECT id_usuario, nome FROM tb_usuario;), faça o uso desse comando para saber qual caractére usar da coluda `id_usuario`, após a validação o programa vai tirar 10 fotos do rosto de vocês em preto e branco e vai adicionar na pasta `faces`(eu fiz um comando no gitbash para que commits futuros não envie fotos do nosso rosto para o github).

/DUDA/
Gente, eu só mudei as pastas do jeito que o maven pede, desse jeito aí fica mais fácil do maven achar o pom, o pom e a foto tem que ficar na pasta principal (SistemaBiometrico), e fiz apenas um pre processamento ele só fala se o caminho é certo ou errado, é apenas para testar se o maven funciona.

/Julya/ 
**Autenticação, RBAC e Persistência MySQL**
- UsuarioSistema: Classe de modelo representando os usuários do sistema e suas permissões.
- Recurso: Mapeamento das funcionalidades e níveis de acesso exigidos.
- Conexao: Gerenciamento de conexão com o banco de dados MySQL (ministerio_meio_ambiente).
- SistemaAutenticacao: Lógica de autenticação, verificação de privilégios e controle de sessões.

 Configuração do Banco:
   - Certifique-se de que o MySQL esteja rodando localmente na porta `3306`.
   - Ajuste as credenciais de acesso na classe `Conexao.java` caso necessário.

**Estrutura do Banco de Dados (MySQL)**
Para configurar o ambiente do MySQL localmente, execute o seguinte script no seu cliente SQL (MySQL Workbench, DBeaver ou terminal):

-- Apaguei a base de dados antiga para evitar conflitos
DROP DATABASE IF EXISTS ministerio_meio_ambiente;

-- Criar e selecionar a base de dados
CREATE DATABASE ministerio_meio_ambiente;
USE ministerio_meio_ambiente;

-- tabela de Usuários
CREATE TABLE tb_usuario (
    id_usuario INT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cargo VARCHAR(50),
    nivel_acesso INT NOT NULL,
    vetor_biometrico VARCHAR(255) NOT NULL
);

-- tabela de Recursos
CREATE TABLE tb_recurso (
    id_recurso INT PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    tipo VARCHAR(50),
    nivel_sigilo INT NOT NULL
);

-- dados de teste
INSERT INTO tb_usuario (id_usuario, nome, cargo, nivel_acesso, vetor_biometrico) VALUES
(1, 'Geovane Luis', 'Analista Ambiental', 1, '0.12,0.45,0.89,0.33'),
(2, 'Dra. Maria Santos', 'Diretora de Divisão', 2, '0.98,0.21,0.11,0.67'),
(3, 'Ministro', 'Ministro do Meio Ambiente', 3, '0.55,0.77,0.33,0.99');

INSERT INTO tb_recurso (id_recurso, descricao, tipo, nivel_sigilo) VALUES
(101, 'Lista de Propriedades Rurais Cadastradas', 'PROPRIEDADE_RURAL', 1),
(102, 'Relatório de Agrotóxicos Banidos e Proibidos', 'AGROTOXICO', 2),
(103, 'Auditoria Confidencial de Crimes Ambientais', 'PROPRIEDADE_RURAL', 3);

-- adicionando tabela de propriedades Rurais

CREATE TABLE tb_propriedade_rural (
    id_propriedade INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    proprietario VARCHAR(100) NOT NULL,
    municipio VARCHAR(80) NOT NULL,
    uf CHAR(2) NOT NULL,
    area_hectares DECIMAL(10,2) NOT NULL,
    situacao VARCHAR(40) NOT NULL,
    nivel_sigilo INT NOT NULL DEFAULT 1
);

INSERT INTO tb_propriedade_rural
    (nome, proprietario, municipio, uf, area_hectares, situacao, nivel_sigilo) VALUES
('Fazenda Boa Esperança', 'João Almeida', 'Rio Verde', 'GO', 1250.50, 'REGULAR', 1),
('Sítio Três Lagoas', 'Maria Oliveira', 'Unaí', 'MG', 85.00, 'REGULAR', 1),
('Fazenda Santa Clara', 'Agropecuária Santa Clara Ltda', 'Sorriso', 'MT', 3400.00, 'EM_ANALISE', 2),
('Fazenda Horizonte', 'Carlos Pereira', 'Luziânia', 'GO', 960.75, 'EM_ANALISE', 2),
('Fazenda Vale Escuro', 'Grupo Vale Escuro S.A.', 'Lucas do Rio Verde', 'MT', 5200.00, 'IRREGULAR', 3),
('Estância Rio Turvo', 'Paulo Ferreira', 'Formosa', 'GO', 2100.30, 'IRREGULAR', 3);

--tabela de log


CREATE TABLE tb_log_acesso (
    id_log INT PRIMARY KEY AUTO_INCREMENT,
    data_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NULL,
    resultado VARCHAR(30) NOT NULL,
    detalhe VARCHAR(255)
);

Compilação e Execução via Maven:
   ```bash
   mvn clean compile
   mvn exec:java -Dexec.mainClass="biometria.SistemaAutenticacao"
