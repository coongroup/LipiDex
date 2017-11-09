package lib_gen;


import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.awt.Toolkit;
import org.apache.commons.lang3.exception.ExceptionUtils;

@SuppressWarnings("serial")
public class CustomError extends JDialog {

	/**
	 * Launch the application.
	 */
	public static void main(String args, Exception ex) {
		try {
			@SuppressWarnings("unused")
			CustomError dialog = new CustomError(args, ex);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	@SuppressWarnings("static-access")
	public CustomError(String message, Exception ex) 
	{
		//Initialize error window
		setTitle("Error");
		setIconImage(Toolkit.getDefaultToolkit().getImage(CustomError.class.getResource("/Icons/Error_Icon.png")));
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
			
			//If exception used for initialization, print stack trace
			if (ex != null)
			{
				ExceptionUtils exUtils = new ExceptionUtils();
				textPane.setText(message+"\n\n\n\n---Error Trace Below---\n\n\n\n"+exUtils.getStackTrace(ex));
			}
			//Else just print error message
			else
				textPane.setText(message);
			
			
			} catch(Exception e) {
				System.out.println("Error setting native LAF: " + e);
			}
			

	}
}
