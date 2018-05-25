package lib_gen;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.*;

import org.apache.commons.io.FileUtils;

import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JTextField;
import javax.swing.ImageIcon;

@SuppressWarnings("serial")
public class LibrarySelector extends JInternalFrame {

	public LipidGenGUI lg;				//Library Generator
	public SpectrumGenerator sg;				//Spectrum Generator
	private JTextField textField;				//Text field for adding new library
	private JList<String> activeLibraryList;	//Currently active libraries
	DefaultListModel<String> model;				//ListModel for active library names
	ArrayList<TransitionType> transitionTypes;	//Arraylist of transitiontypes

	//Constructor for library selector for library generator
	public LibrarySelector(ArrayList<String> availableLibraries, String[] selectedLibrary, 
			LipidGenGUI lg, JDesktopPane contentPane, JLabel label, ImageIcon onImage, 
			ImageIcon offImage) {
		moveToFront();
		setFrameIcon(new ImageIcon("src/icons/lg_16_icon.png"));
		this.lg = lg;
		setClosable(true);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			this.setSelected(true);
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}

		setTitle("Library Selector - Library Generator");
		setSize(567,271);
		setBounds(100, 100, 567, 271);
		getContentPane().setLayout(null);

		JLabel lblSelectLipidLibrary = new JLabel("Available Libraries");
		lblSelectLipidLibrary.setBounds(10, 11, 170, 14);
		getContentPane().add(lblSelectLipidLibrary);

		JScrollPane activeLibrariesScrollPane = new JScrollPane();
		activeLibrariesScrollPane.setBounds(10, 34, 531, 144);
		getContentPane().add(activeLibrariesScrollPane);

		textField = new JTextField();
		textField.setBounds(10, 210, 161, 20);
		getContentPane().add(textField);
		textField.setColumns(10);

		model = new DefaultListModel<String>() {
			String[] values = availableLibraries.toArray(new String[availableLibraries.size()]);
			public int getSize() {
				return values.length;
			}
			public String getElementAt(int index) {
				return values[index];
			}
		};
		activeLibraryList = new JList<String>(model);
		activeLibraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		activeLibrariesScrollPane.setViewportView(activeLibraryList);

		JButton btnChoose = new JButton("Choose Library");
		btnChoose.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				for (int i=0; i<activeLibraryList.getModel().getSize(); i++)
				{
					//If valid library chosen, create new instance of library generator
					if (activeLibraryList.isSelectedIndex(i))
					{
						selectedLibrary[0] = availableLibraries.get(i);
						LipidGenGUI.activeLib = availableLibraries.get(i);
						try {
							setLG(availableLibraries.get(i), contentPane, label, onImage, offImage);
							dispose();
						} catch (Exception e1) {
							CustomError ce = new CustomError("Error loading library", e1);
						}
					}
				}
			}
		});
		
		btnChoose.setBounds(437, 210, 104, 23);
		getContentPane().add(btnChoose);

		JButton btnNewLibrary = new JButton("Create New Library");
		btnNewLibrary.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				//Create folder
				if (!textField.getText().equals(""))
				{
					//Create file objects
					File file = new File("src/libraries/"+textField.getText());
					File adductsFileBackup = new File("src/backup/Adducts.csv");
					File faFileBackup = new File("src/backup/FattyAcids.csv");
					File adductsFile = new File("src/libraries/"+textField.getText()+"/Adducts.csv");
					File faFile = new File("src/libraries/"+textField.getText()+"/FattyAcids.csv");
					File classFile = new File("src/libraries/"+textField.getText()+"/Lipid_Classes.csv");
					File templateFile = new File("src/libraries/"+textField.getText()+"/MS2_Templates.csv");

					//Catch duplicates
					if (file.exists())
					{
						@SuppressWarnings("unused")
						CustomError e1 = new CustomError("Library already exists", null);
					}
					else
					{
						//Make directory
						file.mkdir();

						//Create .csv files
						try {
							FileUtils.copyFile(adductsFileBackup, adductsFile);
							FileUtils.copyFile(faFileBackup, faFile);
							classFile.createNewFile();
							templateFile.createNewFile();
						} catch (IOException e1) {
							e1.printStackTrace();
						}

						//Update ArrayList
						availableLibraries.add(textField.getText());
						model = new DefaultListModel<String>() {
							String[] values = availableLibraries.toArray(new String[availableLibraries.size()]);
							public int getSize() {
								return values.length;
							}
							public String getElementAt(int index) {
								return values[index];
							}
						};
						activeLibraryList.setModel(model);

						//Clear text field
						textField.setText("");
					}
				}

			}
		});
		btnNewLibrary.setBounds(183, 210, 135, 23);
		getContentPane().add(btnNewLibrary);

		JButton btnDeleteLibrary = new JButton("Delete Library");
		btnDeleteLibrary.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				//Find selected library
				for (int i=0; i<activeLibraryList.getModel().getSize(); i++)
				{
					if (activeLibraryList.isSelectedIndex(i) && !availableLibraries.get(i).contains("Coon_Lab") && !availableLibraries.get(i).equals("LipidBlast"))
					{
						String toRemove = availableLibraries.get(i);
						File file = new File("src/libraries/"+toRemove);

						//Remove all files in subdirectors
						String[]entries = file.list();
						for(String s: entries){
							File currentFile = new File(file.getPath(),s);
							currentFile.delete();
						}

						//Remove subdirectories
						file.delete();

						//Remove from list
						availableLibraries.remove(toRemove);

						//Refresh table
						model = new DefaultListModel<String>() {
							String[] values = availableLibraries.toArray(new String[availableLibraries.size()]);
							public int getSize() {
								return values.length;
							}
							public String getElementAt(int index) {
								return values[index];
							}
						};
						activeLibraryList.setModel(model);

						textField.setText("");
					}
				}

			}
		});
		btnDeleteLibrary.setBounds(328, 210, 99, 23);
		getContentPane().add(btnDeleteLibrary);



		JLabel lblNewLibraryName = new JLabel("New Library Name");
		lblNewLibraryName.setBounds(10, 189, 161, 14);
		getContentPane().add(lblNewLibraryName);

		setVisible(true);
	}

	//Constructor for library selector for spectrum generator
	public LibrarySelector(ArrayList<String> availableLibraries, String[] selectedLibrary, SpectrumGenerator sg, 
			JDesktopPane contentPane, JLabel label, ImageIcon onImage, ImageIcon offImage) {
		setFrameIcon(new ImageIcon("src/icons/sg_16_icon.png"));
		setClosable(true);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			this.setSelected(true);
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}

		setTitle("Library Selector - Spectrum Generator");
		setBounds(100, 100, 567, 271);
		getContentPane().setLayout(null);

		JLabel lblSelectLipidLibrary = new JLabel("Available Libraries");
		lblSelectLipidLibrary.setBounds(10, 11, 170, 14);
		getContentPane().add(lblSelectLipidLibrary);

		JScrollPane activeLibrariesScrollPane = new JScrollPane();
		activeLibrariesScrollPane.setBounds(10, 34, 531, 144);
		getContentPane().add(activeLibrariesScrollPane);

		textField = new JTextField();
		textField.setBounds(10, 210, 161, 20);
		getContentPane().add(textField);
		textField.setColumns(10);

		model = new DefaultListModel<String>() {
			String[] values = availableLibraries.toArray(new String[availableLibraries.size()]);
			public int getSize() {
				return values.length;
			}
			public String getElementAt(int index) {
				return values[index];
			}
		};
		activeLibraryList = new JList<String>(model);
		activeLibraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		activeLibrariesScrollPane.setViewportView(activeLibraryList);

		JButton btnChoose = new JButton("Choose Library");
		btnChoose.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				for (int i=0; i<activeLibraryList.getModel().getSize(); i++)
				{
					//Create spectrum generator
					if (activeLibraryList.isSelectedIndex(i))
					{
						try {
							try 
							{
								setSG(availableLibraries.get(i), contentPane, label, onImage, offImage);
							} 
							catch (ClassNotFoundException
									| InstantiationException
									| IllegalAccessException
									| UnsupportedLookAndFeelException e1)
							{
								throw new CustomException("Error selecting library", e1);
							}
							dispose();
						} catch (Exception e1) {
							e1.printStackTrace();
						}

					}
				}
			}
		});
		btnChoose.setBounds(437, 210, 104, 23);
		getContentPane().add(btnChoose);

		JButton btnNewLibrary = new JButton("Create New Library");
		btnNewLibrary.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				//Create folder
				if (!textField.getText().equals(""))
				{
					//Create file objects
					File file = new File("src/libraries/"+textField.getText());
					File adductsFileBackup = new File("src/backup/Adducts.csv");
					File faFileBackup = new File("src/backup/FattyAcids.csv");
					File adductsFile = new File("src/libraries/"+textField.getText()+"/Adducts.csv");
					File faFile = new File("src/libraries/"+textField.getText()+"/FattyAcids.csv");
					File classFile = new File("src/libraries/"+textField.getText()+"/Lipid_Classes.csv");
					File templateFile = new File("src/libraries/"+textField.getText()+"/MS2_Templates.csv");

					//Catch duplicates
					if (file.exists())
					{
						@SuppressWarnings("unused")
						CustomError e1 = new CustomError("Library already exists", null);
					}
					else
					{
						//Make directory
						file.mkdir();

						//Create .csv files
						try {
							FileUtils.copyFile(adductsFileBackup, adductsFile);
							FileUtils.copyFile(faFileBackup, faFile);
							classFile.createNewFile();
							templateFile.createNewFile();
						} catch (IOException e1) {
							e1.printStackTrace();
						}

						//Update ArrayList
						availableLibraries.add(textField.getText());
						model = new DefaultListModel<String>() {
							String[] values = availableLibraries.toArray(new String[availableLibraries.size()]);
							public int getSize() {
								return values.length;
							}
							public String getElementAt(int index) {
								return values[index];
							}
						};
						activeLibraryList.setModel(model);

						//Clear text field
						textField.setText("");
					}
				}

			}
		});
		btnNewLibrary.setBounds(183, 210, 135, 23);
		getContentPane().add(btnNewLibrary);

		JButton btnDeleteLibrary = new JButton("Delete Library");
		btnDeleteLibrary.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				//Find selected library
				for (int i=0; i<activeLibraryList.getModel().getSize(); i++)
				{
					if (activeLibraryList.isSelectedIndex(i) && !availableLibraries.get(i).contains("Coon_Lab") && !availableLibraries.get(i).equals("LipidBlast"))
					{
						String toRemove = availableLibraries.get(i);
						File file = new File("src/libraries/"+toRemove);

						//Remove all files in subdirectors
						String[]entries = file.list();
						for(String s: entries){
							File currentFile = new File(file.getPath(),s);
							currentFile.delete();
						}

						//Remove subdirectories
						file.delete();

						//Remove from list
						availableLibraries.remove(toRemove);

						//Refresh table
						model = new DefaultListModel<String>() {
							String[] values = availableLibraries.toArray(new String[availableLibraries.size()]);
							public int getSize() {
								return values.length;
							}
							public String getElementAt(int index) {
								return values[index];
							}
						};
						activeLibraryList.setModel(model);

						textField.setText("");
					}
				}

			}
		});
		btnDeleteLibrary.setBounds(328, 210, 99, 23);
		getContentPane().add(btnDeleteLibrary);



		JLabel lblNewLibraryName = new JLabel("New Library Name");
		lblNewLibraryName.setBounds(10, 189, 161, 14);
		getContentPane().add(lblNewLibraryName);

		setVisible(true);
	}

	//Creates new instance of library generator and adds to desktop and displays
	public void setLG(String lib, JDesktopPane contentPane, JLabel label, 
			ImageIcon onImage, ImageIcon offImage) throws PropertyVetoException, IOException
	{
			lg = new LipidGenGUI(lib, contentPane, label, onImage, offImage);		
			lg.setVisible(true);
			lg.setIcon(false);
			contentPane.add(lg);
			lg.toFront();
	}

	//Creates new instance of spectrum generator and adds to desktop and displays
	public void setSG(String lib, JDesktopPane contentPane, JLabel label, 
			ImageIcon onImage, ImageIcon offImage) throws PropertyVetoException, 
			ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, CustomException
	{
			sg = new SpectrumGenerator(lib, label, onImage, offImage);
			sg.setVisible(true);
			sg.setIcon(false);
			contentPane.add(sg);
			sg.toFront();
	}
}
