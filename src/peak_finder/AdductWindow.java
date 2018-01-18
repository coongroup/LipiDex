package peak_finder;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JInternalFrame;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JSeparator;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import lib_gen.Adduct;
import lib_gen.CustomError;
import lib_gen.CustomException;
import lib_gen.Utilities;

@SuppressWarnings("serial")
public class AdductWindow extends JInternalFrame {
	private JTable adductTable;
	static ArrayList<Adduct> adductsDB;							//ArrayList of all adducts from active lib
	static Adduct[] adductsArray;								//Array of adducts for table display
	public static Utilities util = new Utilities();				//Instance of Utilities class


	//Launch the application
	public static void main(ArrayList<Adduct> adductArray) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					
					AdductWindow frame = new AdductWindow(adductArray);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//Create frame
	public AdductWindow(ArrayList<Adduct> adductArray) throws IOException {

		AdductWindow.adductsDB = adductArray;
		//Read Adducts
		readAdducts("src/peak_finder/Possible_Adducts.csv");
		
		//Set GUI parameters
		setFrameIcon(new ImageIcon(AdductWindow.class.getResource("/icons/pf_16_icon.png")));
		setTitle("Adduct Filtering");
		setBounds(100, 100, 390, 313);
		//setMinimumSize(new Dimension(601, 356));
		setClosable(true);
		this.setIconifiable(true);
		this.setResizable(true);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		JScrollPane adductScrollPane = new JScrollPane();
		adductTable = new JTable();
		adductTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		adductTable.setGridColor(Color.LIGHT_GRAY);
		DefaultTableModel adductTableModel = new DefaultTableModel(
				renderAdductList(),
				new String[] {
					"Name", "Formula", "Loss", "Polarity", "Charge"
				}
				);
		adductTable.setModel(adductTableModel);
		adductTable.getColumnModel().getColumn(0).setResizable(false);
		adductTable.getColumnModel().getColumn(1).setResizable(false);
		adductTable.getColumnModel().getColumn(2).setResizable(false);
		adductTable.getColumnModel().getColumn(3).setResizable(false);
		adductTable.getColumnModel().getColumn(4).setResizable(false);	
		for (int i=0; i<adductTable.getColumnCount(); i++)
		{
			adductTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
		}

		adductScrollPane.setViewportView(adductTable);

		JButton btnAddNewRow = new JButton("Add Adduct");
		btnAddNewRow.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				adductTableModel.addRow(new Object[]{"", "","", "",""});
			}
		});

		JButton deleteAdduct = new JButton("Delete Adduct");
		deleteAdduct.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				deleteSelectedRows(adductTableModel, adductTable);
			}
		});

		JLabel lblAdductFiltering = new JLabel("Adduct Filtering");

		JButton btnClose = new JButton("Close");
		btnClose.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				updateAdductArrays(adductTableModel, adductTable);
				writeAdductArraytoCSV("src/peak_finder/possible_Adducts.csv");
				dispose();
			}
		});
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(adductScrollPane, GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
								.addComponent(lblAdductFiltering)
								.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
										.addComponent(btnAddNewRow)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(deleteAdduct)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(btnClose)))
										.addContainerGap())
				);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(lblAdductFiltering)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(adductScrollPane, GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
						.addGap(12)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(btnClose)
								.addComponent(deleteAdduct)
								.addComponent(btnAddNewRow))
								.addContainerGap())
				);
		adductTable.getColumnModel().getColumn(0).setPreferredWidth(109);
		adductTable.getColumnModel().getColumn(0).setMinWidth(109);
		adductTable.getColumnModel().getColumn(1).setMinWidth(75);
		adductTable.getColumnModel().getColumn(2).setResizable(false);
		adductTable.getColumnModel().getColumn(2).setPreferredWidth(50);
		adductTable.getColumnModel().getColumn(2).setMinWidth(50);
		adductTable.getColumnModel().getColumn(3).setResizable(false);
		adductTable.getColumnModel().getColumn(3).setPreferredWidth(50);
		adductTable.getColumnModel().getColumn(3).setMinWidth(50);
		adductTable.getColumnModel().getColumn(4).setResizable(false);
		adductTable.getColumnModel().getColumn(4).setPreferredWidth(50);
		adductTable.getColumnModel().getColumn(4).setMinWidth(50);
		adductScrollPane.setViewportView(adductTable);
		getContentPane().setLayout(groupLayout);

	}


	//Load in possible adducts
	private static void readAdducts(String filename) throws IOException
	{
		String line = null;
		String [] split = null;
		String name;
		String formula;
		Boolean loss;
		String polarity;
		int charge;

		//Create file buffer
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));


		//Clear adducts DB
		adductsDB = new ArrayList<Adduct>();

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			if (!line.contains("Name"))
			{
				split = line.split(",");

				name = split[0];

				formula = split[1];

				if (split[2].equals("FALSE")) loss = false;
				else {loss = true;}

				polarity = split[3];
				charge = Integer.valueOf(split[4]);

				adductsDB.add(new Adduct(name, formula, loss, polarity, charge));
			}
		}

		//Create array from arrayList
		adductsArray = adductsDB.toArray(new Adduct[adductsDB.size()]);
		reader.close();
	}

	//Returns object 2d array for adduct table
	private static Object[][] renderAdductList()
	{
		Object[][] result =  new Object[adductsArray.length][3];

		for (int i=0; i<adductsArray.length; i++)
		{
			result[i] = adductsArray[i].getTableArray();
		}

		return result;
	}

	//Write current adduct array to .csv file
	private static void writeAdductArraytoCSV(String filename)
	{
		//Write file
		try {
			PrintWriter pw = new PrintWriter(filename);

			pw.println("Name,Formula,Loss,Polarity,Charge");

			for (int i=0; i<adductsDB.size(); i++)
			{
				pw.println(adductsDB.get(i));
			}

			pw.close();
		} catch (FileNotFoundException e) {
			CustomException error = new CustomException("Error saving adducts to .csv", null);
		}

	}
	
	//Update adduct array from last save adduct .csv
		private static void updateAdductArrays(DefaultTableModel adductTableModel, JTable table)
		{
			ArrayList<Adduct> adductsDBTemp = new ArrayList<Adduct>();
			Adduct[] arrayTemp = new Adduct[adductTableModel.getRowCount()];

			try
			{
				if (!table.isEditing())
				{

					for (int i=0; i<adductTableModel.getRowCount(); i++)
					{
						String[] row = new String[adductTableModel.getColumnCount()];

						for (int j=0; j<adductTableModel.getColumnCount(); j++)
						{
							row[j] = adductTableModel.getValueAt(i, j).toString();
						}

						adductsDBTemp.add(convertAdductArraytoObject(row));
						arrayTemp[i] = convertAdductArraytoObject(row);
					}
				}
				else
				{
					CustomError error = new CustomError("Please finish editing cell before saving table", null);
				}

				adductsDB = adductsDBTemp;
				adductsArray = arrayTemp;
			}
			catch (CustomException e)
			{
				CustomError error = new CustomError(e.getMessage(), null);
			}
		}

		//Converst array table information to adduct object
		private static Adduct convertAdductArraytoObject(String[] array) throws CustomException
		{
			Adduct result = null;
			String name;
			String formula;
			boolean loss;
			String polarity;
			Integer charge;

			//Initialize and check all fields
			//Name
			name = array[0];

			//Formula
			formula = array[1];
			if (!util.validElementalFormula(formula))
				throw new CustomException(formula+" is not a valid elemental formula", null);

			//Loss Boolean
			if (!array[2].equals("true") && !array[2].equals("false")) 
				throw new CustomException(array[2]+" is not a valid loss value.  Value must be true or false", null);
			else loss = Boolean.valueOf(array[2]);

			//Polarity
			if (!array[3].equals("+") && !array[3].equals("-")) 
				throw new CustomException(array[3]+" is not a valid polarity value.  Value must be + or -", null);
			else polarity = array[3];

			//Charge
			if (!array[4].equals("1") && !array[4].equals("2") && !array[3].equals("3") && !array[4].equals("4")) 
				throw new CustomException(array[4]+" is not a valid value for adduct charge.  Value must be an integer between 1-4", null);
			else charge = Integer.valueOf(array[4]);;

			//Initialize
			result = new Adduct(name, formula, loss, polarity, charge);

			return result;
		}

		//Delete selected row from class table
		private static void deleteSelectedRows(DefaultTableModel classTableModel, JTable table)
		{
			if (classTableModel.getRowCount()>0)
			{
				int row = table.getSelectedRow();
				int column = table.getSelectedColumn();
				if (row>-1)
				{
					classTableModel.removeRow(row);
					//Selecting first row
					if (row == 0 && classTableModel.getRowCount() > 0)
					{
						table.getSelectionModel().setSelectionInterval(column, 0);
					}
					//Selecting last row
					else if ((row) > (classTableModel.getRowCount()-1) && classTableModel.getRowCount()>0) 
					{
						table.getSelectionModel().setSelectionInterval(column, row-1);
					}
					//All selections
					else if (classTableModel.getRowCount()>1)
					{
						table.getSelectionModel().setSelectionInterval(column, row);
					}
				}
			}
		}
}
