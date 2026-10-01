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

Compilação e Execução via Maven:
   ```bash
   mvn clean compile
   mvn exec:java -Dexec.mainClass="biometria.SistemaAutenticacao"
