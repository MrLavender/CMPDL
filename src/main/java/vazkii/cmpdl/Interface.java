package vazkii.cmpdl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

public class Interface {

	private static Frame frame;
	public static OperatorThread operatorThread;

	private static String line1 = "";
	private static String line2 = "";
	
	public static void openInterface() {
		frame = new Frame();
		setStatus("Idle");
	}

	public static void addLogLine(String s) {
		if(frame != null) {
			String currText = frame.logArea.getText();
			frame.logArea.setText(currText.isEmpty() ? s : (currText + "\n" + s));
		}
	}

	public static void setStatus(String status) {
		setStatus(status, true);
	}
	
	public static void setStatus(String status, boolean clear) {
		line1 = status;
		if(clear)
			setStatus2("");
		else updateLabel();
	}
	
	public static void setStatus2(String status) {
		line2 = status;
		updateLabel();
	}
	
	private static void updateLabel() {
		if(frame != null)
			frame.currentStatus.setText(String.format("<html>%s<br>%s</html>", line1, line2));
	}
	
	@SuppressWarnings("deprecation")
	public static void finishDownload(boolean killThread) {
		if(operatorThread != null && operatorThread.isAlive() && killThread) {
			operatorThread.interrupt();
			operatorThread.stop();
			operatorThread = null;
		}
		
		if(frame != null)
			frame.downloadButton.setText("Download");
		
		CMPDL.downloading = false;
	}
	
	public static void error() {
		finishDownload(false);
		setStatus("Errored");
	}

	static final class Frame extends JFrame implements ActionListener, KeyListener {

		private static final long serialVersionUID = -2280547253170432552L;

		JPanel panel;
		JPanel downloadPanel;
		JPanel urlPanel;
		JPanel versionPanel;
		JPanel statusPanel;
		JButton downloadButton;
		JLabel urlLabel;
		JLabel versionLabel;
		JScrollPane scrollPane;
		JTextField urlField;
		JTextField versionField;
		JTextArea logArea;
		JLabel currentStatus;
		JButton clearButton;

		public Frame() {
			setSize(800, 640);
			setTitle("Vazkii's Curse Modpack Downloader (CMPDL)");

			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			downloadPanel = new JPanel();
			downloadPanel.setLayout(new BoxLayout(downloadPanel, BoxLayout.LINE_AXIS));
			downloadPanel.setMaximumSize(new Dimension(1000, 100));
			urlPanel = new JPanel();
			urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.PAGE_AXIS));
			versionPanel = new JPanel();
			versionPanel.setLayout(new BoxLayout(versionPanel, BoxLayout.PAGE_AXIS));
			statusPanel = new JPanel();
			downloadButton = new JButton("Download");
			downloadButton.setAlignmentX(CENTER_ALIGNMENT);
			urlLabel = new JLabel("Modpack URL :");
			urlField = new JTextField("", 54);
			versionLabel = new JLabel("Curse File ID :");
			versionField = new JTextField("latest", 20);

			logArea = new JTextArea(34, 68);
			logArea.setBackground(Color.WHITE);
			logArea.setEditable(false);
			logArea.setLineWrap(true);

			scrollPane = new JScrollPane(logArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

			Border scrollBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10),
					BorderFactory.createLineBorder(Color.GRAY, 1));
			scrollPane.setBorder(scrollBorder);
			DefaultCaret caret = (DefaultCaret) logArea.getCaret();
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

			currentStatus = new JLabel("", SwingConstants.LEFT);

			clearButton = new JButton("Clear Output");
			clearButton.setAction(new AbstractAction("Clear Output") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					logArea.setText("");
				}
			});

			urlPanel.add(urlLabel);
			urlPanel.add(Box.createRigidArea(new Dimension(20, 5)));
			urlPanel.add(urlField);
			urlPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

			versionPanel.add(versionLabel);
			versionPanel.add(Box.createRigidArea(new Dimension(26, 5)));
			versionPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
			versionPanel.add(versionField);

			downloadPanel.add(urlPanel);
			downloadPanel.add(versionPanel);
			panel.add(downloadPanel);
			panel.add(downloadButton);
			panel.add(scrollPane);
			statusPanel.setLayout(new BorderLayout());
			statusPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
			statusPanel.setMaximumSize(new Dimension(1000, 200));
			statusPanel.add(clearButton, BorderLayout.WEST);
			statusPanel.add(currentStatus, BorderLayout.EAST);
			panel.add(statusPanel);
			add(panel);

			downloadButton.requestFocus();
			downloadButton.addActionListener(this);
			urlField.addKeyListener(this);
			versionField.addKeyListener(this);

			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					System.exit(1);
				}
			});

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch(Exception ex) {
				ex.printStackTrace();
			}

			setResizable(true);
			setVisible(true);

			DropHandler dropHandler = new DropHandler();
			panel.setTransferHandler(dropHandler);
			urlField.setTransferHandler(dropHandler);
			logArea.setTransferHandler(dropHandler);
		}

		@Override
		public void keyTyped(KeyEvent e) {
			if(e.getKeyChar() == '\n')
				actionPerformed(null);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == downloadButton) {
				boolean downloading = CMPDL.downloading;
				if(downloading && operatorThread != null) {
					Interface.finishDownload(true);
					Interface.setStatus("Stopped Manually");
				} else {
					String url = urlField.getText();
					String version = versionField.getText();
					if(url != null && !url.isEmpty() && !downloading) {
						operatorThread = new OperatorThread(url, version);
						((JButton) e.getSource()).setText("Stop");	
					}
				}
			}

		}

		@Override public void keyPressed(KeyEvent e) {	}
		@Override public void keyReleased(KeyEvent e) { }

		private final class DropHandler extends TransferHandler {

			private final boolean DND_DEBUG = Boolean.getBoolean("cmpdl.dndDebug");

			private final DataFlavor DATAFLAVOR_URI_LIST = getUriListFlavor();

			private DataFlavor getUriListFlavor() {
				try {
					return new DataFlavor("text/uri-list;class=java.lang.String");
				} catch (ClassNotFoundException e) {
				}
				return null;
			}

			@Override
			public boolean canImport(TransferHandler.TransferSupport support) {
				return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
					|| support.isDataFlavorSupported(DataFlavor.stringFlavor)
					|| support.isDataFlavorSupported(DATAFLAVOR_URI_LIST)
					|| DND_DEBUG;
			}

			@Override
			public boolean importData(TransferHandler.TransferSupport support) {
				Transferable t = support.getTransferable();
				if (DND_DEBUG) {
					printDataFlavors(t);
				}

				String url = getDroppedItem(t);
				if (url != null) {
					urlField.setText(url);
					versionField.setText("latest");
					return true;
				}

				String msg = "Drop or paste a modpack URL or local zip file";
				SwingUtilities.invokeLater(() -> {
					frame.setAlwaysOnTop(true);
					JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
					frame.setAlwaysOnTop(false);
				});
				return false;
			}

			private String getDroppedItem(Transferable t) {
				List<Function<Transferable, String>> dataGetters = Arrays.asList(
					this::getFileListData, this::getStringData, this::getUriListData
				);

				for (Function<Transferable, String> dataGetter : dataGetters) {
					String data = dataGetter.apply(t);
					if (data != null) {
						for (String item : data.split("\\r?\\n")) {
							if ((item = validateDroppedItem(item)) != null) {
								return item;
							}
						}
					}
				}
				return null;
			}

			private String validateDroppedItem(String item) {
				item = item.trim();
				try {
					URL url = new URL(item);
					if (!url.getProtocol().equals("file") || item.endsWith(".zip")) {
						return url.toString();
					}
				} catch (MalformedURLException e) {
					if (item.endsWith(".zip")) {
						return "file://" + item;
					}
				}
				return null;
			}

			private String getFileListData(Transferable t) {
				if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					try {
						List<?> data = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
						return data.stream().map(Object::toString).collect(Collectors.joining("\n"));
					} catch (UnsupportedFlavorException | IOException e) {
					}
				}
				return null;
			}

			private String getStringData(Transferable t) {
				if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					try {
						return (String) t.getTransferData(DataFlavor.stringFlavor);
					} catch (UnsupportedFlavorException | IOException e) {
					}
				}
				return null;
			}

			private String getUriListData(Transferable t) {
				if (t.isDataFlavorSupported(DATAFLAVOR_URI_LIST)) {
					try {
						return (String) t.getTransferData(DATAFLAVOR_URI_LIST);
					} catch (UnsupportedFlavorException | IOException e) {
					}
				}
				return null;
			}

			private void printDataFlavors(Transferable t) {
				for (DataFlavor flavor : t.getTransferDataFlavors()) {
					System.out.println(flavor);
				}
				System.out.println("-----------------------------------------");
				System.out.println("FILE LIST");
				System.out.println(getFileListData(t));
				System.out.println("-----------------------------------------");
				System.out.println("STRING");
				System.out.println(getStringData(t));
				System.out.println("-----------------------------------------");
				System.out.println("URI LIST");
				System.out.println(getUriListData(t));
				System.out.println("-----------------------------------------");
				System.out.println("-----------------------------------------");
			}
		}
	}

}
