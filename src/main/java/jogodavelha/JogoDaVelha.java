package jogodavelha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class JogoDaVelha implements Runnable {

	private String ip = "localhost";
	private int porta = 22222;
	private Scanner scanner = new Scanner(System.in);
	private JFrame frame;
	private final int WIDTH = 506;
	private final int HEIGHT = 527;
	private Thread thread;

	private Painter painter;
	private Socket socket;
	private DataOutputStream dos; 
	private DataInputStream dis;

	private ServerSocket serverSocket;

	private BufferedImage board;
	private BufferedImage Xvermelho;
	private BufferedImage Xazul;
	private BufferedImage CirculoVermelho;
	private BufferedImage CirculoAzul;

	private String[] espacos = new String[9];

	private boolean seuTurno = false;
	private boolean circulo = true;
	private boolean aceito = false;
	private boolean OponenteIncomunicavel = false;
	private boolean ganhou = false;
	private boolean inimigoGanhou = false;
	private boolean empate = false;

	private int comprimentoEspaco = 160;
	private int erros = 0;
	private int firstSpot = -1;
	private int secondSpot = -1;

	private Font fonte = new Font("Verdana", Font.BOLD, 32);
	private Font fonteMenor = new Font("Verdana", Font.BOLD, 20);
	private Font fonteMaior = new Font("Verdana", Font.BOLD, 40);

	private String esperandoString = "Esperando por outro jogador.";
	private String OponenteIncomunicavelString = "Foi perdida a conexão com o oponente.";
	private String empateString = "Empate!";

	private int[][] vitorias = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };

	/*
	 * 0, 1, 2 
	 * 3, 4, 5 
	 * 6, 7, 8
	*/

	public JogoDaVelha() {																		// Recebe o ip e a porta e monta a interface
		System.out.println("Informe o ip: ");
		ip = scanner.nextLine();
		System.out.println("Informe a porta: ");
		porta = scanner.nextInt();
		while (porta < 1 || porta > 65535) {
			System.out.println("A porta inserida é invalida, insira uma porta valida: ");
			porta = scanner.nextInt();
		}

		carregarImagens();

		painter = new Painter();
		painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

		if (!conectar()) iniciarServer();

		frame = new JFrame();
		frame.setTitle("Tic-Tac-Toe");
		frame.setContentPane(painter);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		thread = new Thread(this, "Jogo-Da-Velha");
		thread.start();
	}

	public void run() {
		while (true) {
			tick();
			painter.repaint();																	// Vai recarregando a interface conforme o jogo.

			if ((!circulo && !aceito) || OponenteIncomunicavel) {
				escutarSolicitacaoServer();
			}
		}
	}

	private void render(Graphics g) {
		g.drawImage(board, 0, 0, null);
		if (OponenteIncomunicavel) {													// Faz o texto se perder a conexao com o outro jogador 
			g.setColor(Color.RED);
			g.setFont(fonteMenor);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int comprimentoString = g2.getFontMetrics().stringWidth(OponenteIncomunicavelString);
			g.drawString(OponenteIncomunicavelString, WIDTH / 2 - comprimentoString / 2, HEIGHT / 2);
			return;
		}

		if (aceito) {
			for (int i = 0; i < espacos.length; i++) {											// Insere as imagens dos 'X' e 'O' conforme o jogo
				if (espacos[i] != null) {
					if (espacos[i].equals("X")) {
						if (circulo) {
							g.drawImage(Xvermelho, (i % 3) * comprimentoEspaco + 10 * (i % 3), (int) (i / 3) * comprimentoEspaco + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(Xazul, (i % 3) * comprimentoEspaco + 10 * (i % 3), (int) (i / 3) * comprimentoEspaco + 10 * (int) (i / 3), null);
						}
					} else if (espacos[i].equals("O")) {
						if (circulo) {
							g.drawImage(CirculoAzul, (i % 3) * comprimentoEspaco + 10 * (i % 3), (int) (i / 3) * comprimentoEspaco + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(CirculoVermelho, (i % 3) * comprimentoEspaco + 10 * (i % 3), (int) (i / 3) * comprimentoEspaco + 10 * (int) (i / 3), null);
						}
					}
				}
			}
			if (ganhou || inimigoGanhou) {																// Faz a linha ligando os 3 simbolos se tiver um ganhador
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(10));
				g.setColor(Color.WHITE);
				g.drawLine(firstSpot % 3 * comprimentoEspaco + 10 * firstSpot % 3 + comprimentoEspaco / 2, (int) (firstSpot / 3) * comprimentoEspaco + 10 * (int) (firstSpot / 3) + comprimentoEspaco / 2, secondSpot % 3 * comprimentoEspaco + 10 * secondSpot % 3 + comprimentoEspaco / 2, (int) (secondSpot / 3) * comprimentoEspaco + 10 * (int) (secondSpot / 3) + comprimentoEspaco / 2);

			}
			if (empate) {																			// Escreve "Empate!" se tiver um empate
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.WHITE);
				g.setFont(fonteMaior);
				int comprimentoString = g2.getFontMetrics().stringWidth(empateString);
				g.drawString(empateString, WIDTH / 2 - comprimentoString / 2, HEIGHT / 2);
			}
		} else {																				// Escreve que está esperando por outro jogador se não tiver outro jogador
			g.setColor(Color.WHITE);
			g.setFont(fonte);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int comprimentoString = g2.getFontMetrics().stringWidth(esperandoString);
			g.drawString(esperandoString, WIDTH / 2 - comprimentoString / 2, HEIGHT / 2);
		}

	}

	private void tick() {
		if (erros >= 10) OponenteIncomunicavel = true;								//Verifica se a conexao foi perdida

		if (!seuTurno && !OponenteIncomunicavel) {									// Verifica a jogada do oponente
			try {
				int space = dis.readInt();
				if (circulo) espacos[space] = "X";
				else espacos[space] = "O";
				checarVitoriaOponente();
				checarEmpate();
				seuTurno = true;
			} catch (IOException e) {
				e.printStackTrace();
				erros++;
			}
		}
	}

	private void checharVitoria() {																//Verifica se o jogador ganhou
		for (int i = 0; i < vitorias.length; i++) {
			if (circulo) {
				if (espacos[vitorias[i][0]] == "O" && espacos[vitorias[i][1]] == "O" && espacos[vitorias[i][2]] == "O") {
					firstSpot = vitorias[i][0];
					secondSpot = vitorias[i][2];
					ganhou = true;
				}
			} else {
				if (espacos[vitorias[i][0]] == "X" && espacos[vitorias[i][1]] == "X" && espacos[vitorias[i][2]] == "X") {
					firstSpot = vitorias[i][0];
					secondSpot = vitorias[i][2];
					ganhou = true;
				}
			}
		}
	}

	private void checarVitoriaOponente() {															//Verifica se o oponente ganhou
		for (int i = 0; i < vitorias.length; i++) {
			if (circulo) {
				if (espacos[vitorias[i][0]] == "X" && espacos[vitorias[i][1]] == "X" && espacos[vitorias[i][2]] == "X") {
					firstSpot = vitorias[i][0];
					secondSpot = vitorias[i][2];
					inimigoGanhou = true;
				}
			} else {
				if (espacos[vitorias[i][0]] == "O" && espacos[vitorias[i][1]] == "O" && espacos[vitorias[i][2]] == "O") {
					firstSpot = vitorias[i][0];
					secondSpot = vitorias[i][2];
					inimigoGanhou = true;
				}
			}
		}
	}

	private void checarEmpate() {																//Verifica se teve um empate
		for (int i = 0; i < espacos.length; i++) {
			if (espacos[i] == null) {
				return;
			}
			if (ganhou || inimigoGanhou)
				return;
		}
		empate = true;
	}

	private void escutarSolicitacaoServer() {														//Verifica o pedido de conexao
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			aceito = true;
			OponenteIncomunicavel = false;
			System.out.println("Cliente solicitou conexao e foi conectado.");
			}
		 	catch (IOException e) {
			e.printStackTrace();
		 	}
	}

	private boolean conectar() {																	//Tenta fazer a conexao
		while (true) {
			try {
				socket = new Socket(ip, porta);
				dos = new DataOutputStream(socket.getOutputStream());
				dis = new DataInputStream(socket.getInputStream());
				aceito = true;
			} catch (IOException e) {
				System.out.println("Nao foi possivel conectar no endereco: " + ip + ":" + porta + " | Iniciando um servidor");
				return false;
			}
			System.out.println("Conectado no servidor com sucesso.");
			return true;
		}	
	}

	private void iniciarServer() {															//Inicia um servidor se nao for possivel conectar a um
		try {
			serverSocket = new ServerSocket(porta, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		seuTurno = true;
		circulo = false;
	}

	private void carregarImagens() {																	//Carrega as imagens
		try {
			board = ImageIO.read(getClass().getResourceAsStream("/Velha.png"));
			Xvermelho = ImageIO.read(getClass().getResourceAsStream("/Xvermelho.png"));
			CirculoVermelho = ImageIO.read(getClass().getResourceAsStream("/Overmelho.png"));
			Xazul = ImageIO.read(getClass().getResourceAsStream("/Xazul.png"));
			CirculoAzul = ImageIO.read(getClass().getResourceAsStream("/Oazul.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		JogoDaVelha jogodavelha = new JogoDaVelha();
	}

	private class Painter extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.DARK_GRAY);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		public void mouseClicked(MouseEvent e) {												//Verifica a jogada do jogador e permite se for a vez dele
			if (aceito) {
				if (seuTurno && !OponenteIncomunicavel && !ganhou && !inimigoGanhou) {
					int x = e.getX() / comprimentoEspaco;
					int y = e.getY() / comprimentoEspaco;
					y *= 3;
					int position = x + y;

					if (espacos[position] == null) {
						if (!circulo) espacos[position] = "X";
						else espacos[position] = "O";
						seuTurno = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							dos.writeInt(position);
							dos.flush();
						} catch (IOException e1) {
							erros++;
							e1.printStackTrace();
						}

						System.out.println("DADOS ENVIADOS");
						checharVitoria();
						checarEmpate();

					}
				}
			}
		}

		public void mousePressed(MouseEvent e) {

		}

		public void mouseReleased(MouseEvent e) {

		}

		public void mouseEntered(MouseEvent e) {

		}

		public void mouseExited(MouseEvent e) {

		}

	}

}
