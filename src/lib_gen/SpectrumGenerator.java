package lib_gen;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTextField;


@SuppressWarnings("serial")
public class SpectrumGenerator extends JInternalFrame {

	private final JPanel contentPanel = new JPanel();			//Main GUI JPanel
	private static JTable spectrumTable;						//Table for displaying spectra
	public static ArrayList<MS2Template> ms2Templates = null;	//Arraylist of all ms2 templates from active library
	public static MS2Template currentMS2Template = null;		//Currently active ms2 template
	public static JTextField textField;							//Textfield  for precursor mass
	static ArrayList<LipidClass> lipidClassesDB = 
			new ArrayList<LipidClass>();						//Arraylist of all lipid classes from active library
	static ArrayList<Adduct> adductsDB = 
			new ArrayList<Adduct>();							//Arraylist of all adducts from active library
	static ArrayList<FattyAcid> fattyAcidsDB = 
			new ArrayList<FattyAcid>();							//Arraylist of all fatty acids from active library
	static ArrayList<ConsensusLipidClass> consensusClasses = 
			new ArrayList<ConsensusLipidClass>();				//Arraylist of all class adduct combos from active library
	static ArrayList<TransitionType> transitionTypes; 			//Arraylist of all class adduct combos from active library
	static ArrayList<String> transitionTypeStrings = 
			new ArrayList<String>();							//Arraylist of all types of ms2 transitions possible

	//Launch window
	@SuppressWarnings("unused")
	public static void main(String activeLib, JLabel label, 
			ImageIcon onImage, ImageIcon offImage, ArrayList<TransitionType> transitionTypes) {
		try {
			SpectrumGenerator dialog = new SpectrumGenerator(activeLib, label, onImage, offImage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//SpectrumGenerator constructor
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SpectrumGenerator(String activeLib, JLabel label, 
			ImageIcon onImage, ImageIcon offImage) throws ClassNotFoundException, 
			InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, CustomException {

		//Populate lipid classes from active library
		readFattyAcids("src/libraries/"+activeLib+"\\FattyAcids.csv");
		readAdducts("src/libraries/"+activeLib+"\\Adducts.csv");
		readClass("src/libraries/"+activeLib+"\\Lipid_Classes.csv");
		populateFattyAcids();
		populateConsensusClasses();
		ms2Templates = uploadTemplates(false, false, "src/libraries/"+activeLib+"\\MS2_Templates.csv");

		//Set GUI parameters
		setClosable(true);
		this.setIconifiable(true);
		setFrameIcon(new ImageIcon("src/icons/sg_16_icon.png"));
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		setTitle("Spectrum Generator");
		setBounds(100, 100, 415, 460);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		//Initialize fatty acid boxes
		JComboBox sn1ComboBox = new JComboBox();
		sn1ComboBox.setEnabled(false);
		sn1ComboBox.setBounds(10, 121, 87, 20);
		contentPanel.add(sn1ComboBox);

		JComboBox sn2ComboBox = new JComboBox();
		sn2ComboBox.setEnabled(false);
		sn2ComboBox.setBounds(107, 121, 87, 20);
		contentPanel.add(sn2ComboBox);

		JComboBox sn3ComboBox = new JComboBox();
		sn3ComboBox.setEnabled(false);
		sn3ComboBox.setBounds(204, 121, 87, 20);
		contentPanel.add(sn3ComboBox);

		JComboBox sn4ComboBox = new JComboBox();
		sn4ComboBox.setEnabled(false);
		sn4ComboBox.setBounds(301, 121, 87, 20);
		contentPanel.add(sn4ComboBox);

		JButton okButton = new JButton("Generate Spectrum");
		okButton.setEnabled(false);
		okButton.setBounds(10, 193, 125, 23);
		okButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				try {
					generateLipid(currentMS2Template, sn1ComboBox,sn2ComboBox,sn3ComboBox,sn4ComboBox, transitionTypes);
				} catch (Exception e1) {
					CustomError ce = new CustomError ("Error generating spectrum",e1);
				}
			}
		});

		contentPanel.add(okButton);

		//Change menu icon when closed
		addInternalFrameListener(new InternalFrameAdapter()
		{
			public void internalFrameClosing(InternalFrameEvent e) 
			{
				if (offImage != null) label.setIcon(offImage);
			}
		});

		//Change menu icon when opened
		addInternalFrameListener(new InternalFrameAdapter()
		{
			public void internalFrameOpened(InternalFrameEvent e) 
			{
				if (offImage != null) label.setIcon(onImage);
			}
		});

		//Populate dropdown list with all classes in active library
		JComboBox classComboBox = new JComboBox();
		classComboBox.setBounds(10, 36, 184, 20);
		populateFragClassList(classComboBox);

		classComboBox.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) 
			{
				populateFAComboBox(classComboBox, sn1ComboBox,sn2ComboBox,sn3ComboBox,sn4ComboBox);
				okButton.setEnabled(true);
			}
		});
		contentPanel.add(classComboBox);

		//Initialize gui elements
		JLabel lblStepChoose = new JLabel("Step 1: Choose lipid class");
		lblStepChoose.setBounds(10, 11, 152, 14);
		contentPanel.add(lblStepChoose);

		JSeparator separator = new JSeparator();
		separator.setBounds(10, 24, 378, 2);
		contentPanel.add(separator);

		JLabel lblStepChoose_1 = new JLabel("Step 2: Choose fatty acids");
		lblStepChoose_1.setBounds(10, 82, 152, 14);
		contentPanel.add(lblStepChoose_1);

		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(10, 95, 378, 2);

		JSeparator separator_2 = new JSeparator();
		separator_2.setBounds(10, 180, 378, 2);
		contentPanel.add(separator_2);

		JLabel lblStepGenerate = new JLabel("Step 3: Generate spectrum");
		lblStepGenerate.setBounds(10, 167, 152, 14);
		contentPanel.add(lblStepGenerate);

		JScrollPane spectrumTablePane = new JScrollPane();
		spectrumTablePane.setBounds(10, 251, 379, 160);
		contentPanel.add(spectrumTablePane);

		spectrumTable = new JTable();
		spectrumTable.setModel(new DefaultTableModel(
				new Object[][] {
						{null, null},
				},
				new String[] {
						"Mass", "Relative Intensity"
				}
				) {
			boolean[] columnEditables = new boolean[] {
					false, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		spectrumTable.getColumnModel().getColumn(0).setResizable(false);
		spectrumTable.getColumnModel().getColumn(1).setResizable(false);
		spectrumTablePane.setViewportView(spectrumTable);

		JLabel lblSn = new JLabel("sn1");
		lblSn.setBounds(10, 106, 46, 14);
		contentPanel.add(lblSn);

		JLabel lblSn_1 = new JLabel("sn2");
		lblSn_1.setBounds(107, 107, 46, 14);
		contentPanel.add(lblSn_1);

		JLabel lblSn_2 = new JLabel("sn3");
		lblSn_2.setBounds(204, 106, 46, 14);
		contentPanel.add(lblSn_2);

		JLabel lblSn_3 = new JLabel("sn4");
		lblSn_3.setBounds(301, 106, 46, 14);
		contentPanel.add(lblSn_3);

		JSeparator separator_3 = new JSeparator();
		separator_3.setBounds(10, 95, 378, 2);
		contentPanel.add(separator_3);

		JLabel lblPrecursorMass = new JLabel("Precursor Mass:");
		lblPrecursorMass.setBounds(10, 230, 87, 14);
		contentPanel.add(lblPrecursorMass);

		textField = new JTextField();
		textField.setEditable(false);
		textField.setEnabled(false);
		textField.setBounds(95, 227, 86, 20);
		contentPanel.add(textField);
		textField.setColumns(10);

		setVisible(true);
	}

	//Method to populate dropdown menu with all active lipid classes
	public static void populateFragClassList(JComboBox<String> box)
	{
		ArrayList<String> menu = new ArrayList<String>();
		menu.add("");

		//Popualte menu
		for (int i=0; i<ms2Templates.size(); i++)
		{
			menu.add(ms2Templates.get(i).lipidClass.name);
		}

		String[] menuArray = new String[menu.size()];
		menu.toArray(menuArray);

		box.setModel(new DefaultComboBoxModel<String>(menuArray));
	}

	//Method to populate dropdown menus with all active fatty acids
	public static void populateFAComboBox(JComboBox<FattyAcid> classBox,
			JComboBox<FattyAcid> sn1ComboBox, 
			JComboBox<FattyAcid> sn2ComboBox, 
			JComboBox<FattyAcid> sn3ComboBox, 
			JComboBox<FattyAcid> sn4ComboBox)
	{
		if (!String.valueOf(classBox.getSelectedItem()).equals(""))
		{
			//Find current ms2 template
			for (int i=0; i<ms2Templates.size(); i++)
			{
				//If match found, set as current ms2 template
				if (ms2Templates.get(i).lipidClass.name.equals(String.valueOf(classBox.getSelectedItem())))
				{
					currentMS2Template = ms2Templates.get(i);
					break;
				}
			}

			//Get array of all possible fatty acids
			ArrayList<ArrayList<FattyAcid>> faArray = currentMS2Template.lipidClass.lClass.possibleFA;

			//Sort array
			for (int i=0; i<faArray.size(); i++)
			{
				Collections.sort(faArray.get(i));
			}

			//Set visibility of fatty acid drop down boxes and populate
			if (faArray.size()>0)
			{
				sn1ComboBox.setEnabled(true);
				sn2ComboBox.setEnabled(false);
				sn3ComboBox.setEnabled(false);
				sn4ComboBox.setEnabled(false);												//Set visibility
				ArrayList<FattyAcid> menu = new ArrayList<FattyAcid>(); 					//Create array							
				for (int i=0; i<faArray.get(0).size(); i++)									//Iterate through fa's
				{
					menu.add(faArray.get(0).get(i));										//add to menu
				}
				FattyAcid[] menuArray = new FattyAcid[menu.size()];							//Create array
				menu.toArray(menuArray);													//Create array
				sn1ComboBox.setModel(new DefaultComboBoxModel<FattyAcid>(menuArray)); 		//Set box model
			}
			if (faArray.size()>1)
			{
				sn1ComboBox.setEnabled(true);
				sn2ComboBox.setEnabled(true);
				sn3ComboBox.setEnabled(false);
				sn4ComboBox.setEnabled(false);
				ArrayList<FattyAcid> menu = new ArrayList<FattyAcid>(); 					//Create array														
				for (int i=0; i<faArray.get(1).size(); i++)									//Iterate through fa's
				{
					menu.add(faArray.get(1).get(i));										//add to menu
				}
				FattyAcid[] menuArray = new FattyAcid[menu.size()];							//Create array
				menu.toArray(menuArray);													//Create array
				sn2ComboBox.setModel(new DefaultComboBoxModel<FattyAcid>(menuArray)); 		//Set box model
			}
			if (faArray.size()>2)
			{
				sn1ComboBox.setEnabled(true);
				sn2ComboBox.setEnabled(true);
				sn3ComboBox.setEnabled(true);
				sn4ComboBox.setEnabled(false);
				ArrayList<FattyAcid> menu = new ArrayList<FattyAcid>(); 					//Create array													
				for (int i=0; i<faArray.get(2).size(); i++)									//Iterate through fa's
				{
					menu.add(faArray.get(2).get(i));										//add to menu
				}
				FattyAcid[] menuArray = new FattyAcid[menu.size()];							//Create array
				menu.toArray(menuArray);													//Create array
				sn3ComboBox.setModel(new DefaultComboBoxModel<FattyAcid>(menuArray)); 		//Set box model
			}
			if (faArray.size()>3)
			{
				sn1ComboBox.setEnabled(true);
				sn2ComboBox.setEnabled(true);
				sn3ComboBox.setEnabled(true);
				sn4ComboBox.setEnabled(true);
				ArrayList<FattyAcid> menu = new ArrayList<FattyAcid>(); 					//Create array														
				for (int i=0; i<faArray.get(3).size(); i++)									//Iterate through fa's
				{
					menu.add(faArray.get(3).get(i));										//add to menuu
				}
				FattyAcid[] menuArray = new FattyAcid[menu.size()];							//Create array
				menu.toArray(menuArray);													//Create array
				sn4ComboBox.setModel(new DefaultComboBoxModel<FattyAcid>(menuArray)); 		//Set box model
			}


		}
	}

	//Generate lipid from selected ms2 template and fatty acids
	public static void generateLipid(MS2Template template,
			JComboBox<FattyAcid> sn1ComboBox, 
			JComboBox<FattyAcid> sn2ComboBox, 
			JComboBox<FattyAcid> sn3ComboBox, 
			JComboBox<FattyAcid> sn4ComboBox,
			ArrayList<TransitionType> transitionTypes) throws CustomException
	{
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();

		//Create FA Array and populate
		ArrayList<FattyAcid> faTemp = new ArrayList<FattyAcid>();
		if (sn1ComboBox.isEnabled()) faTemp.add((FattyAcid)sn1ComboBox.getModel().getSelectedItem());
		if (sn2ComboBox.isEnabled()) faTemp.add((FattyAcid)sn2ComboBox.getModel().getSelectedItem());
		if (sn3ComboBox.isEnabled()) faTemp.add((FattyAcid)sn3ComboBox.getModel().getSelectedItem());
		if (sn4ComboBox.isEnabled()) faTemp.add((FattyAcid)sn4ComboBox.getModel().getSelectedItem());

		Lipid lipidTemp = new Lipid (faTemp, currentMS2Template.lipidClass.lClass, currentMS2Template.lipidClass.adduct);
		MS2 ms2 = currentMS2Template.generateMS2(lipidTemp, transitionTypes);

		//Add generated transitions to table
		for (int i=0; i<ms2.transitions.size(); i++)
		{
			ArrayList<String> tempArray = new ArrayList<>(Arrays.asList(
					"   "+String.valueOf(Math.round (Double.valueOf(ms2.transitions.get(i).mass) * 10000.0) / 10000.0), 
					"   "+String.valueOf(Math.round(ms2.transitions.get(i).getIntensity()))));

			result.add(tempArray);
		}

		String[][] resultArray = new String[result.size()][2];

		for (int i=0; i<result.size(); i++)
		{
			resultArray[i][0] = result.get(i).get(0);
			resultArray[i][1] = result.get(i).get(1);
		}

		//Enable precursor Fields
		textField.setEnabled(true);

		//Populate Field
		textField.setText(String.valueOf(Math.round(ms2.precursor*10000.0)/10000.0));

		//Populate Table
		spectrumTable.setModel(new DefaultTableModel(
				resultArray,
				new String[] {
						"   Mass", "  Relative Intensity"
				}
				) {
			boolean[] columnEditables = new boolean[] {
					false, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});

		//Format table
		spectrumTable.setGridColor(Color.LIGHT_GRAY);
	}

	//For all lipid classes, generate all possible fatty acid combinations
	public static void populateFattyAcids()
	{
		for (int i=0; i<lipidClassesDB.size(); i++)
		{
			lipidClassesDB.get(i).populateFattyAcids(fattyAcidsDB);
		}
	}

	//Generate all valid lipid class + adduct combinations
	public static void populateConsensusClasses()
	{
		consensusClasses = new ArrayList<ConsensusLipidClass>();

		//Iterate through lipidClass
		for (int i=0; i<lipidClassesDB.size(); i++)
		{
			//Iterate through adducts
			for (int j=0; j<lipidClassesDB.get(i).adducts.length; j++)
			{
				//Create new ConsensusClass
				consensusClasses.add(new ConsensusLipidClass(lipidClassesDB.get(i),lipidClassesDB.get(i).adducts[j]));
			}
		}
	}

	//Genreate ms2 template objects from .csv record
	public static ArrayList<MS2Template> uploadTemplates(boolean generateLipids, 
			boolean updateProgress, String filename) throws IOException
			{
		Double intensity;
		TransitionDefinition transitionTemp;
		String formulaTemp;
		String[] split;
		String line;
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		ArrayList<TransitionDefinition> transitions = new ArrayList<TransitionDefinition>();
		ConsensusLipidClass lipidClass = null;
		MS2Template tempMS2Template;
		ArrayList<MS2Template> result = new ArrayList<MS2Template>();


		//Clear MS2Templates
		ms2Templates = new ArrayList<MS2Template>();

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			//If line is a new lipid template
			if (line.contains("]"))
			{
				//Initialize transition array
				transitions = new ArrayList<TransitionDefinition>();

				//Search for lipidclass to associate
				for (int i=0; i<consensusClasses.size(); i++)
				{
					if (line.equals(consensusClasses.get(i).getName()))
					{
						lipidClass = consensusClasses.get(i);
					}
				}
			}

			//If end of entry, add to array
			else if (!line.equals(""))
			{
				if (line.contains("---"))
				{
					tempMS2Template = new MS2Template(lipidClass, transitions);
					result.add(tempMS2Template);
				}
				else
				{
					split = line.split(",");
					formulaTemp = split[0];
					intensity = Double.valueOf(split[1]);
					transitionTemp = new TransitionDefinition(formulaTemp, intensity, line, split[3], 
							Integer.valueOf(split[2]), getTransitionType(split[3]));
					transitions.add(transitionTemp);
				}

			}
		}

		reader.close();

		//Generate all lipid objects if exporting
		if (generateLipids) 
			for (int i=0; i<result.size(); i++)
			{
				if (result.get(i).transitions.size() > 0)
					result.get(i).generateLipids();
			}

		return result;
			}

	//Load in possible fatty acids
	public static void readFattyAcids(String filename) throws IOException
	{
		String line = null;
		String [] split = null;
		String name;
		String type;
		String enabled;
		String formula;
		ArrayList<String> faTypes = new ArrayList<String>();

		//Create file buffer
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));

		//Clear FA DB
		fattyAcidsDB = new ArrayList<FattyAcid>();

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			if (!line.contains("Name"))
			{
				split = line.split(",");

				name = split[0];
				type = split[1];
				formula = split[2];
				enabled = split[3];

				fattyAcidsDB.add(new FattyAcid(name, type, formula, enabled));

				if (!faTypes.contains(type)) faTypes.add(type);
			}
		}

		//Create transition type objects
		transitionTypes = createTransitionTypes(faTypes);

		//Populate string array
		for (int i=0; i<transitionTypes.size(); i++)
		{
			transitionTypeStrings.add(transitionTypes.get(i).toString());
		}
		reader.close();
	}

	//Returns array of all transition type definition objects for given fatty acids
	private static ArrayList<TransitionType> createTransitionTypes(ArrayList<String> typeArray)
	{
		ArrayList<TransitionType> definitions = new ArrayList<TransitionType>();

		//Create static fragment class
		definitions.add(new TransitionType("Fragment",null,false,false,0));

		//Create static neutral loss class
		definitions.add(new TransitionType("Neutral Loss",null,false,true,0));

		//For all fatty acid type strings
		for (int i=0; i<typeArray.size(); i++)
		{
			//Create moiety fragment class
			definitions.add(new TransitionType(typeArray.get(i)+" Fragment",typeArray.get(i),true,false,1));

			//Create moiety neutral loss class
			definitions.add(new TransitionType(typeArray.get(i)+" Neutral Loss",typeArray.get(i),true,true,1));
		}

		//Create cardiolipin DG fragment class
		definitions.add(new TransitionType("Cardiolipin DG Fragment","Alkyl",true,false,2));

		//Create PUFA neutral loss class
		definitions.add(new TransitionType("PUFA Fragment","PUFA",true,false,1));

		//Create PUFA fragment class
		definitions.add(new TransitionType("PUFA Neutral Loss","PUFA",true,true,1));

		return definitions;
	}

	//Load in possible adducts
	public static void readAdducts(String filename) throws IOException
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
		reader.close();
	}

	//Load in possible lipid classes
	public static void readClass(String filename) throws IOException
	{
		String line = null;
		String [] split = null;
		String [] adductSplit;
		Adduct[] adductArray = null;
		String name;
		String abbrev;
		String head;
		boolean sterol;
		boolean glycerol;
		boolean sphingoid;
		String backboneFormula;
		int numFA;
		String polarity;
		ArrayList<String> faTypes;
		boolean newFormat = false;

		//Create file buffer
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));


		//Clear Class DB
		lipidClassesDB = new ArrayList<LipidClass>();

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
			//Check if library is new or old format
			if (line.contains("Name") && line.contains("Backbone"))
				newFormat = true;


			if (!line.contains("Name"))
			{
				split = line.split(",");

				name = split[0];
				abbrev = split[1];
				head = split[2];

				adductSplit = split[3].split(";");
				adductArray = new Adduct[adductSplit.length];

				for (int i=0; i<adductSplit.length; i++)
				{
					for (int j=0; j<adductsDB.size(); j++)
					{
						if (adductsDB.get(j).getName().equals(adductSplit[i]))
						{
							adductArray[i] = adductsDB.get(j);
						}
					}
				}

				if (split[4].equals("FALSE")) sterol = false;
				else {sterol = true;}

				if (split[5].equals("FALSE")) glycerol = false;
				else {glycerol = true;}

				if (split[6].equals("FALSE")) sphingoid = false;
				else {sphingoid = true;}

				if (newFormat)
					backboneFormula = split[7];
				else
					backboneFormula = "";

				if (newFormat)
					numFA = Integer.valueOf(split[8]);
				else
					numFA = Integer.valueOf(split[7]);

				if (newFormat)
					polarity = split[9];
				else
					polarity = split[8];

				//Parse fatty acid types
				faTypes = new ArrayList<String>();

				if (newFormat)
				{
					for (int i=10; i<split.length; i++)
					{
						if (!split[i].equals("none"))
						{
							faTypes.add(split[i]);
						}
					}
				}
				else
				{
					for (int i=9; i<split.length; i++)
					{
						if (!split[i].equals("none"))
						{
							faTypes.add(split[i]);
						}
					}
				}
				lipidClassesDB.add(new LipidClass(name, abbrev, head, adductArray, sterol, glycerol, sphingoid, backboneFormula, numFA, polarity, faTypes));
			}
		}
		reader.close();
	}

	//Return transition type object corresponding to provided string
	private static TransitionType getTransitionType(String s)
	{
		for (int i=0; i<transitionTypes.size(); i++)
		{
			if (transitionTypes.get(i).name.equals(s)) return transitionTypes.get(i);
		}

		return null;
	}
}
