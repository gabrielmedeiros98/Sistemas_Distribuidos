# Sistemas_Distribuidos

# Descrição do Projeto

A ideia do projeto a ser implementado na disciplina é um Jogo da Velha (TicTacToe) no qual será possível dois usuários se enfrentarem.
O funcionamento será basicamente os usuários entrarem no game na mesma "sala" e começarem a jogar, o sistema indicará quem venceu ou se terminou empatado.

# Componentes

Cliente
Servidor
Aplicação 

# Testes

Teste de Concorrência: Vários clientes podem criar salas de ate dois jogadores sem nenhum problema.
Teste de ponta a ponta: As jogadas do oponente podem ser assistidas pelo palyer.
Teste de recuperação de falhas: Quando a sessão cair por algum motivo inesperado, o sistema deve continuar com as jogadas feitas até ali salvas.
Testede funcionalidades: Todas as funcionalidades deverão estar ok.
Exemplo: Os campos de marcação do X ou do O devem estar funcionando, as mensagens confirmando o vencedor e perguntando se gostariam de continuar jogando e a regra do jogo, quem marcar uma sequencia de 3 posições primeiro, vence. 
