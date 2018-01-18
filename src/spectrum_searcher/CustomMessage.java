package spectrum_searcher;


import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import java.awt.Color;

@SuppressWarnings("serial")
public class CustomMessage extends JDialog {

	/**
	 * Launch the application.
	 */
	public static void main(String args) {
		try {
			@SuppressWarnings("unused")
			CustomMessage dialog = new CustomMessage(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public CustomMessage(String message) 
	{
		//Initialize error window
		setTitle("Message");
		try {
			JScrollPane scrollPane_1 = new JScrollPane();
			setMinimumSize(new Dimension(400, 300));
			scrollPane_1.setBounds(0, 0, 400, 300);
			getContentPane().add(scrollPane_1);
			
			JTextPane textPane = new JTextPane();
			textPane.setBackground(Color.WHITE);
			textPane.setText(message);
			textPane.setEditable(false);
			scrollPane_1.setViewportView(textPane);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			setVisible(true);
			
				textPane.setText(message);
			
			
			} catch(Exception e) {
				System.out.println("Error setting native LAF: " + e);
			}
			

	}
}
