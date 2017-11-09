package lib_gen;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;
import javax.swing.JInternalFrame;
import javax.swing.JDesktopPane;
import javax.swing.JTable;
import javax.swing.JProgressBar;
import javax.swing.JTree;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import javax.swing.DefaultComboBoxModel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ListSelectionModel;

import java.awt.Font;
import java.beans.PropertyVetoException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import compound_discoverer.CDPeakFinder;


@SuppressWarnings("serial")
public class LipidGenGUI extends JInternalFrame {

	//GUI Variables
	private JPanel contentPane;						//Main JPanel
	private static JTable fattyAcidTable;			//Table of potential fatty acids
	private JTextField outputField;					//Text field for generate library file destination	
	private static JTable outputTable;				//Table containing all libraries available for export
	private JTable adductTable;						//Jtable for adducts
	private JTextField massFormulaField;			//Field for mass/formula of fragmentation rule
	private JTextField relativeIntField;			//Field for relative intensity of fragmentation rule
	public static JTree tree;						//JTree for browsing fragmentation rules
	DefaultMutableTreeNode selectedNode;			//Currently selected node (frag rule)
	DefaultMutableTreeNode selectedParentNode;		//Currently selected node (lipid class)
	static JProgressBar progressBar = null;			//Library gen progress bar
	static JProgressBar outputProgressBar;			//Progress bar for library creation
	private JTextField chargeField;					//Field for charge of fragmentation rule
	private SwingWorker<Void, Void> worker;			//Swingworker for handing PeakFinder algorithms

	//Non-GUI Variables
	static ArrayList<LipidClass> lipidClassesDB = 
			new ArrayList<LipidClass>();						//ArrayList of all lipid classes from active lib
	static ArrayList<Adduct> adductsDB = 						
			new ArrayList<Adduct>();							//ArrayList of all adducts from active lib
	static ArrayList<FattyAcid> fattyAcidsDB = 
			new ArrayList<FattyAcid>();							//ArrayList of all fatty acids from active lib
	static ArrayList<String> allFattyAcidTypes = 
			new ArrayList<String>();							//Arraylist of all fatty acid types from active lib
	static ArrayList<TransitionType> transitionTypes = 
			new ArrayList<TransitionType>();					//Arraylist of all types of ms2 transitions possible
	static ArrayList<String> transitionTypeStrings = 
			new ArrayList<String>();							//Arraylist of all types of ms2 transitions possible
	static LipidClass[] lipidClassesArray;						//Array of classes for table display
	static Adduct[] adductsArray;								//Array of adducts for table display
	static FattyAcid[] fattyAcidsArray;							//Array offatty acids for table display
	static ArrayList<ConsensusLipidClass> consensusClasses = 
			new ArrayList<ConsensusLipidClass>();				//ArrayList of consensus classes
	static ArrayList<String> fragClassList = 
			new ArrayList<String>();							//ArrayList of all lipid classes for frag rule menu
	public static ArrayList<MS2Template> ms2Templates = 
			new ArrayList<MS2Template>();						//ArrayList of all ms2Template objects
	static MS2Template[] ms2TemplateArray;						//Array of all ms2Template objects for table display
	public static String activeLib = "";						//Current active library
	public static Utilities util = new Utilities();				//Instance of Utilities class
	String outputDir = "";										//Directory for exporting libraries

	//Constructor
	@SuppressWarnings({ "static-access", "unchecked", "rawtypes" })
	public LipidGenGUI(String activeLib, JDesktopPane mainContentPane, 
			JLabel label, ImageIcon onImage, ImageIcon offImage) throws IOException 
	{
		//Set active library
		this.activeLib = activeLib;

		//Set GUI parameters
		this.setMaximumSize(new Dimension(1260,700));
		this.setMinimumSize(new Dimension(610,501));
		setFrameIcon(new ImageIcon(LipidGenGUI.class.getResource("/icons/lg_16_icon.png")));
		setClosable(true);
		this.setIconifiable(true);
		setResizable(true);
		setTitle("Library Generator");
		setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
		setBounds(100, 100, 614, 539);

		//Change menu bar icon when closed
		addInternalFrameListener(new InternalFrameAdapter()
		{
			public void internalFrameClosing(InternalFrameEvent e) 
			{
				label.setIcon(offImage);
			}
		});

		//Change menu bar icon when opened
		addInternalFrameListener(new InternalFrameAdapter()
		{
			public void internalFrameOpened(InternalFrameEvent e) 
			{
				label.setIcon(onImage);
			}
		});

		try {
			this.setSelected(true);
		} catch (PropertyVetoException e2) {
			e2.printStackTrace();
		}

		//Load in all configuration files
		try {
			readFattyAcids("src/libraries/"+activeLib+"\\FattyAcids.csv");
			readAdducts("src/libraries/"+activeLib+"\\Adducts.csv");
			readClass("src/libraries/"+activeLib+"\\Lipid_Classes.csv");

		} catch (IOException e1) {
			@SuppressWarnings("unused")
			CustomError e = new CustomError ("Error reading library files. Please check library formatting", null); 
			e1.printStackTrace();
		}

		//Set look and feel to user default
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}

		//Initialize lipid class table
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

		JPanel lipid_classes = new JPanel();
		tabbedPane.addTab("Lipid Classes", null, lipid_classes, null);

		JScrollPane classTablePane = new JScrollPane();
		classTablePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		DefaultTableModel classTableModel = new DefaultTableModel(
				renderClassList(),
				new String[] {
					"Name", "Abbreviation", "Head Group", "Adducts", "Backbone", "Num. Fatty Acids", "Optimal Polarity", "sn1", "sn2", "sn3", "sn4"
				}
				) {
			Class[] columnTypes = new Class[] {
					String.class, String.class, String.class, String.class, 
					String.class, String.class, String.class, String.class, 
					String.class, String.class, String.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		};



		JTable classTable = new JTable(classTableModel);
		classTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		classTable.setGridColor(Color.LIGHT_GRAY);
		classTablePane.setViewportView(classTable);
		classTable.setAutoResizeMode(1);
		classTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		classTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		classTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(3).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(4).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(5).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(6).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(7).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(8).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(9).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(10).setPreferredWidth(100);
		classTable.getColumnModel().getColumn(0).setMinWidth(50);
		classTable.getColumnModel().getColumn(1).setMinWidth(50);
		classTable.getColumnModel().getColumn(2).setMinWidth(50);
		classTable.getColumnModel().getColumn(3).setMinWidth(50);
		classTable.getColumnModel().getColumn(4).setMinWidth(50);
		classTable.getColumnModel().getColumn(5).setMinWidth(50);
		classTable.getColumnModel().getColumn(6).setMinWidth(50);
		classTable.getColumnModel().getColumn(7).setMinWidth(50);
		classTable.getColumnModel().getColumn(8).setMinWidth(50);
		classTable.getColumnModel().getColumn(9).setMinWidth(50);
		classTable.getColumnModel().getColumn(10).setMinWidth(50);

		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		for (int i=0; i<classTable.getColumnCount(); i++)
		{
			classTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
		}

		JLabel lblLipidClassTable = new JLabel("Lipid Class Table");

		JButton classTableDeleteButton = new JButton("Delete Row");
		classTableDeleteButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				deleteSelectedRows(classTableModel, classTable);
			}
		});

		JButton classTableNewButton = new JButton("New Row");
		classTableNewButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				classTableModel.addRow(new Object[]{"", "", "","", "", "","", "", "","", ""});
			}
		});

		JComboBox<String> classDropDown = new JComboBox<String>();
		populateFragClassList(classDropDown);
		JButton classTableSaveButton = new JButton("Save");
		classTableSaveButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				updateClassArrays(classTableModel,classTable);
				writeClassArraytoCSV("src/libraries/"+activeLib+"\\Lipid_Classes.csv");
				populateFragClassList(classDropDown);
				updateOutputTable();
			}
		});

		JButton classTableResetButton = new JButton("Reset");
		classTableResetButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				resetClassTable(classTableModel);
			}
		});

		//Initialize fragmentation rule window
		DefaultTreeModel libNode = new DefaultTreeModel(new DefaultMutableTreeNode("Test"));
		tree = new JTree(libNode);
		tree.setModel(renderFragList(tree,false));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				selectedNode = (DefaultMutableTreeNode)
						tree.getLastSelectedPathComponent();
				if (selectedNode == null) return;
				else
				{
					selectedParentNode = (DefaultMutableTreeNode)selectedNode.getParent();
				}

				/* if nothing is selected */ 
				if (selectedNode == null) return;
			}
		});
		DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

		tree.setFont(new Font("Courier New", Font.PLAIN, 12));
		DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
		tree.setCellRenderer(renderer);
		model.reload();
		expandAllNodes(tree);

		GroupLayout gl_lipid_classes = new GroupLayout(lipid_classes);
		gl_lipid_classes.setHorizontalGroup(
				gl_lipid_classes.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_lipid_classes.createSequentialGroup()
						.addGap(10)
						.addComponent(classTablePane, GroupLayout.DEFAULT_SIZE, 571, Short.MAX_VALUE))
						.addGroup(gl_lipid_classes.createSequentialGroup()
								.addGap(195)
								.addComponent(classTableNewButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
								.addGap(10)
								.addComponent(classTableDeleteButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
								.addGap(10)
								.addComponent(classTableResetButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
								.addGap(10)
								.addComponent(classTableSaveButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE))
								.addGroup(Alignment.LEADING, gl_lipid_classes.createSequentialGroup()
										.addContainerGap()
										.addComponent(lblLipidClassTable, GroupLayout.PREFERRED_SIZE, 102, GroupLayout.PREFERRED_SIZE)
										.addContainerGap(469, Short.MAX_VALUE))
				);
		gl_lipid_classes.setVerticalGroup(
				gl_lipid_classes.createParallelGroup(Alignment.CENTER)
				.addGroup(gl_lipid_classes.createSequentialGroup()
						.addGap(10)
						.addComponent(lblLipidClassTable)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(classTablePane, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
						.addGap(11)
						.addGroup(gl_lipid_classes.createParallelGroup(Alignment.LEADING)
								.addComponent(classTableNewButton)
								.addComponent(classTableDeleteButton)
								.addComponent(classTableResetButton)
								.addComponent(classTableSaveButton)))
				);
		lipid_classes.setLayout(gl_lipid_classes);


		//Initialize fatty acid table
		DefaultTableModel faTableModel = createFAModel();
		JPanel adducts = new JPanel();
		tabbedPane.addTab("Adducts", null, adducts, null);


		//Initialize adduct table
		JLabel lblAdducts = new JLabel("Adducts");
		JSeparator separator_4 = new JSeparator();
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

		JButton addAdduct = new JButton("Add Adduct");
		addAdduct.addActionListener(new ActionListener() 
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


		JButton adductResetButton = new JButton("Reset");
		adductResetButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				resetAdductTable(adductTableModel);
			}
		});


		JButton adductSaveButton = new JButton("Save");
		adductSaveButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				updateAdductArrays(adductTableModel, adductTable);
				writeAdductArraytoCSV("src/libraries/"+activeLib+"\\Adducts.csv");
				populateFragClassList(classDropDown);
				updateOutputTable();
			}
		});
		GroupLayout gl_adducts = new GroupLayout(adducts);
		gl_adducts.setHorizontalGroup(
				gl_adducts.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_adducts.createSequentialGroup()
						.addGap(10)
						.addGroup(gl_adducts.createParallelGroup(Alignment.LEADING)
								.addComponent(lblAdducts, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
								.addGroup(gl_adducts.createSequentialGroup()
										.addGap(45)
										.addComponent(separator_4, GroupLayout.PREFERRED_SIZE, 1, GroupLayout.PREFERRED_SIZE))
										.addComponent(adductScrollPane, GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE))
										.addGap(10)
										.addGroup(gl_adducts.createParallelGroup(Alignment.LEADING)
												.addComponent(addAdduct, GroupLayout.PREFERRED_SIZE, 101, GroupLayout.PREFERRED_SIZE)
												.addComponent(deleteAdduct)
												.addComponent(adductResetButton, GroupLayout.PREFERRED_SIZE, 101, GroupLayout.PREFERRED_SIZE)
												.addComponent(adductSaveButton, GroupLayout.PREFERRED_SIZE, 101, GroupLayout.PREFERRED_SIZE)))
				);
		gl_adducts.setVerticalGroup(
				gl_adducts.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_adducts.createSequentialGroup()
						.addGap(11)
						.addGroup(gl_adducts.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_adducts.createSequentialGroup()
										.addComponent(lblAdducts)
										.addGap(137)
										.addComponent(separator_4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
										.addGroup(gl_adducts.createSequentialGroup()
												.addGap(13)
												.addComponent(adductScrollPane, GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE))))
												.addGroup(gl_adducts.createSequentialGroup()
														.addGap(137)
														.addComponent(addAdduct)
														.addGap(11)
														.addComponent(deleteAdduct)
														.addGap(11)
														.addComponent(adductResetButton)
														.addGap(11)
														.addComponent(adductSaveButton))
				);
		adducts.setLayout(gl_adducts);
		ImageIcon icon = new ImageIcon("src/icons/Book_Icon_White.png");
		ImageIcon icon2 = new ImageIcon("src/icons/Leaf_Icon.png");
		renderer.setClosedIcon(icon);
		renderer.setOpenIcon(icon);
		renderer.setLeafIcon(icon2);

		//Initialize fatty acid table
		JPanel fatty_acids = new JPanel();
		tabbedPane.addTab("Fatty Acids", null, fatty_acids, null);
		JLabel lblFattyAcidTable = new JLabel("Fatty Acid Table");
		JScrollPane fattyAcidTablePane = new JScrollPane();
		fattyAcidTable = new JTable();
		fattyAcidTable.setForeground(Color.BLACK);
		fattyAcidTable.setGridColor(Color.LIGHT_GRAY);
		fattyAcidTable.setModel(new DefaultTableModel(
				new Object[][] {
						{null, null, null, null},
				},
				new String[] {
						"Name", "Type", "Enabled", "Formula"
				}
				) {
			boolean[] columnEditables = new boolean[] {
					true, true, true, true
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		fattyAcidTable.getColumnModel().getColumn(0).setResizable(false);
		fattyAcidTable.getColumnModel().getColumn(1).setResizable(false);
		fattyAcidTable.getColumnModel().getColumn(2).setResizable(false);
		fattyAcidTable.getColumnModel().getColumn(3).setResizable(false);

		fattyAcidTable.setModel(faTableModel);
		fattyAcidTablePane.setViewportView(fattyAcidTable);

		JButton selectAllFA = new JButton("Select All");
		selectAllFA.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				selectAll(faTableModel);
			}
		});

		JButton desectAllFA = new JButton("Deselect All");
		desectAllFA.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				deselectAll(faTableModel);
			}
		});

		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				updateFAArrays((DefaultTableModel)fattyAcidTable.getModel());
				writeFAArraytoCSV("src/libraries/"+activeLib+"\\FattyAcids.csv");
				try {
					readFattyAcids("src/libraries/"+activeLib+"\\FattyAcids.csv");
				} catch (IOException e1) {
					CustomError ce = new CustomError ("Error saving fatty acids", e1);
				}
				updateOutputTable();
			}
		});

		JButton btnReset = new JButton("Reset");
		btnReset.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				fattyAcidTable.setModel(createFAModel());
			}
		});

		JButton btnDeleteEntry = new JButton("Delete");
		btnDeleteEntry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				deleteSelectedFARows((DefaultTableModel)fattyAcidTable.getModel(), fattyAcidTable);
			}
		});
		JButton btnAddEntry = new JButton("Add");
		btnAddEntry.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				((DefaultTableModel)fattyAcidTable.getModel()).addRow(new Object[]{"", "", "",false});
			}
		});


		GroupLayout gl_fatty_acids = new GroupLayout(fatty_acids);
		gl_fatty_acids.setHorizontalGroup(
				gl_fatty_acids.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_fatty_acids.createSequentialGroup()
						.addGap(10)
						.addGroup(gl_fatty_acids.createParallelGroup(Alignment.LEADING)
								.addComponent(lblFattyAcidTable, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
								.addGroup(Alignment.TRAILING, gl_fatty_acids.createSequentialGroup()
										.addGroup(gl_fatty_acids.createParallelGroup(Alignment.TRAILING)
												.addGroup(gl_fatty_acids.createSequentialGroup()
														.addPreferredGap(ComponentPlacement.RELATED, 269, Short.MAX_VALUE)
														.addComponent(btnSave, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(btnReset, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE))
														.addComponent(fattyAcidTablePane, GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE))
														.addPreferredGap(ComponentPlacement.UNRELATED)
														.addGroup(gl_fatty_acids.createParallelGroup(Alignment.TRAILING)
																.addGroup(gl_fatty_acids.createSequentialGroup()
																		.addComponent(desectAllFA, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
																		.addContainerGap())
																		.addGroup(gl_fatty_acids.createSequentialGroup()
																				.addGroup(gl_fatty_acids.createParallelGroup(Alignment.LEADING, false)
																						.addComponent(selectAllFA, GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE)
																						.addComponent(btnDeleteEntry, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																						.addComponent(btnAddEntry, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
																						.addContainerGap())))))
				);
		gl_fatty_acids.setVerticalGroup(
				gl_fatty_acids.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_fatty_acids.createSequentialGroup()
						.addGap(11)
						.addComponent(lblFattyAcidTable)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_fatty_acids.createParallelGroup(Alignment.LEADING)
								.addGroup(Alignment.TRAILING, gl_fatty_acids.createSequentialGroup()
										.addComponent(btnAddEntry)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(btnDeleteEntry)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(selectAllFA)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(desectAllFA))
										.addComponent(fattyAcidTablePane, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 397, GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addGroup(gl_fatty_acids.createParallelGroup(Alignment.BASELINE)
												.addComponent(btnReset)
												.addComponent(btnSave))
												.addContainerGap())
				);
		fatty_acids.setLayout(gl_fatty_acids);

		//Initialize fragmentation rule browser
		JPanel frag_rules = new JPanel();
		tabbedPane.addTab("Fragmentation Rules", null, frag_rules, null);
		JScrollPane fragRulePane = new JScrollPane();
		fragRulePane.setViewportView(tree);
		JLabel lblAddFragmentationRule = new JLabel("Fragmentation Rule Editor");
		JLabel lblFragmentationRules = new JLabel("Fragmentation Rules \r\n(Mass/Formula, Intensity, Charge , Type)");
		JSeparator separator_5 = new JSeparator();
		JButton saveRules = new JButton("Save");
		saveRules.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				saveFragRules("src/libraries/"+activeLib+"\\MS2_Templates.csv");
				updateOutputTable();
			}
		});

		JButton resetRules = new JButton("Reset");
		resetRules.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				try {
					ms2Templates = uploadTemplates(false, false,"src/libraries/"+activeLib+"\\MS2_Templates.csv");
					tree.setModel(renderFragList(tree,false));
					expandAllNodes(tree);
				} catch (IOException e1) {
					CustomError error = new CustomError(e1.getMessage(), null);
				}
			}
		});

		JLabel lblLipidClass = new JLabel("Lipid Class");
		massFormulaField = new JTextField();
		massFormulaField.setColumns(10);
		JLabel lblNewLabel = new JLabel("Mass or Formula");
		JLabel lblTransitionType = new JLabel("Transition Type");
		JLabel lblRelIntensity = new JLabel("Rel. Intensity");
		relativeIntField = new JTextField();
		relativeIntField.setColumns(10);
		JButton deleteSelectedBTN = new JButton("Delete Selected");
		deleteSelectedBTN.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				deleteFragRule(tree);
			}
		});

		JComboBox transitionTypeBTN = new JComboBox();
		transitionTypeBTN.setModel(new DefaultComboBoxModel(transitionTypeStrings.toArray(new String[transitionTypeStrings.size()])));

		JButton addNewRuleBTN = new JButton("Add New Rule");
		addNewRuleBTN.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				try {
					addFragRule(classDropDown.getSelectedItem().toString(), 
							massFormulaField.getText(), transitionTypeBTN.getSelectedItem().toString(), 
							relativeIntField.getText(), Integer.valueOf(chargeField.getText()));
					tree.setModel(renderFragList(tree,true));
					expandAllNodes(tree);
				} catch (Exception error) {
					CustomError e1 = new CustomError(error.getMessage(), null);
				}
			}
		});

		JButton editSelectedBTN = new JButton("Edit Selected");
		editSelectedBTN.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				try {
					populateFragEditFields (selectedNode, selectedParentNode, 
							massFormulaField, relativeIntField, transitionTypeBTN, classDropDown, chargeField);
				} catch (Exception error) {
					CustomError e1 = new CustomError(error.getMessage(), null);
				}
			}
		});

		JButton updateSelectedButton = new JButton("Update Selected");
		updateSelectedButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				try {
					updateFragRule (tree,selectedNode, selectedParentNode, 
							massFormulaField, relativeIntField, transitionTypeBTN, classDropDown, chargeField);
					tree.setModel(renderFragList(tree,true));
					expandAllNodes(tree);
				} catch (Exception error) {
					//Error e1 = new Error(error.getMessage(), null, null);
					error.printStackTrace();
				}
			}
		});

		JButton btnGenerateExampleSpectra = new JButton("Create Example Spectra");
		btnGenerateExampleSpectra.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				try {
					//Create progress dialog
					//Load in all configuration files
					readFattyAcids("src/libraries/"+activeLib+"\\FattyAcids.csv");
					readAdducts("src/libraries/"+activeLib+"\\Adducts.csv");
					readClass("src/libraries/"+activeLib+"\\Lipid_Classes.csv");
					populateFattyAcids();
					populateConsensusClasses();
					ms2Templates = uploadTemplates(false, false, "src/libraries/"+activeLib+"\\MS2_Templates.csv");

					//Create spectrum generator window
					SpectrumGenerator sg = new SpectrumGenerator(activeLib, null, null, null);
					mainContentPane.add(sg);
					sg.toFront();

				} catch (Exception error) {
					CustomError e1 = new CustomError(error.getMessage(), error);
				}
			}
		});

		JLabel lblCharge = new JLabel("Charge");

		chargeField = new JTextField();
		chargeField.setColumns(10);

		GroupLayout gl_frag_rules = new GroupLayout(frag_rules);
		gl_frag_rules.setHorizontalGroup(
				gl_frag_rules.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_frag_rules.createSequentialGroup()
						.addGap(10)
						.addComponent(lblFragmentationRules, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
						.addGap(10)
						.addComponent(lblAddFragmentationRule, GroupLayout.PREFERRED_SIZE, 149, GroupLayout.PREFERRED_SIZE)
						.addGap(12))
						.addGroup(gl_frag_rules.createSequentialGroup()
								.addGroup(gl_frag_rules.createParallelGroup(Alignment.LEADING)
										.addGroup(gl_frag_rules.createSequentialGroup()
												.addGap(10)
												.addComponent(fragRulePane, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
												.addGap(10))
												.addGroup(gl_frag_rules.createSequentialGroup()
														.addGap(114)
														.addComponent(saveRules, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
														.addGap(10)
														.addComponent(resetRules, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
														.addGap(118)))
														.addGroup(gl_frag_rules.createParallelGroup(Alignment.LEADING)
																.addComponent(separator_5, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
																.addComponent(lblLipidClass, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE)
																.addComponent(classDropDown, GroupLayout.PREFERRED_SIZE, 149, GroupLayout.PREFERRED_SIZE)
																.addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 86, GroupLayout.PREFERRED_SIZE)
																.addComponent(massFormulaField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
																.addComponent(lblTransitionType, GroupLayout.PREFERRED_SIZE, 103, GroupLayout.PREFERRED_SIZE)
																.addComponent(transitionTypeBTN, GroupLayout.PREFERRED_SIZE, 149, GroupLayout.PREFERRED_SIZE)
																.addComponent(lblRelIntensity, GroupLayout.PREFERRED_SIZE, 86, GroupLayout.PREFERRED_SIZE)
																.addComponent(relativeIntField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
																.addComponent(lblCharge, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
																.addComponent(chargeField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
																.addComponent(addNewRuleBTN, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
																.addComponent(editSelectedBTN, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
																.addComponent(updateSelectedButton, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
																.addComponent(deleteSelectedBTN, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
																.addComponent(btnGenerateExampleSpectra, GroupLayout.PREFERRED_SIZE, 151, GroupLayout.PREFERRED_SIZE))
																.addContainerGap())
				);
		gl_frag_rules.setVerticalGroup(
				gl_frag_rules.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_frag_rules.createSequentialGroup()
						.addGap(13)
						.addGroup(gl_frag_rules.createParallelGroup(Alignment.LEADING)
								.addComponent(lblFragmentationRules)
								.addComponent(lblAddFragmentationRule))
								.addGap(3)
								.addGroup(gl_frag_rules.createParallelGroup(Alignment.LEADING)
										.addComponent(fragRulePane, GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
										.addGroup(gl_frag_rules.createSequentialGroup()
												.addComponent(separator_5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												.addGap(6)
												.addComponent(lblLipidClass)
												.addGap(7)
												.addComponent(classDropDown, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												.addGap(11)
												.addComponent(lblNewLabel)
												.addComponent(massFormulaField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												.addGap(11)
												.addComponent(lblTransitionType)
												.addGap(4)
												.addComponent(transitionTypeBTN, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												.addGap(11)
												.addComponent(lblRelIntensity)
												.addGap(2)
												.addComponent(relativeIntField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												.addGap(11)
												.addComponent(lblCharge)
												.addComponent(chargeField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												.addGap(11)
												.addComponent(addNewRuleBTN)
												.addGap(11)
												.addComponent(editSelectedBTN)
												.addGap(11)
												.addComponent(updateSelectedButton)
												.addGap(11)
												.addComponent(deleteSelectedBTN)
												.addGap(11)
												.addComponent(btnGenerateExampleSpectra)))
												.addGap(4)
												.addGroup(gl_frag_rules.createParallelGroup(Alignment.LEADING)
														.addComponent(saveRules)
														.addComponent(resetRules))
														.addContainerGap())
				);
		frag_rules.setLayout(gl_frag_rules);

		//Initialize library generation tab
		JPanel library_gen = new JPanel();
		tabbedPane.addTab("Library Generation", null, library_gen, null);
		JLabel lblLibraryGeneration = new JLabel("Library Generation");
		JLabel lblOutputDirectory = new JLabel("Step 2 (Optional): Select alternate output directory");
		outputField = new JTextField();
		outputField.setColumns(10);
		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setAcceptAllFileFilterUsed(false);
				chooser.setCurrentDirectory(new File("C:"));
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					outputField.setText(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});

		JLabel lblOutputType = new JLabel("Step 1: Select output type");

		JRadioButton combinedOutput = new JRadioButton("Combined .msp");
		combinedOutput.setSelected(true);

		JRadioButton separateOutput = new JRadioButton("Separate .msp files");

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(combinedOutput);
		buttonGroup.add(separateOutput);

		JScrollPane outputPane = new JScrollPane();

		outputTable = new JTable();
		outputTable.setShowGrid(true);
		outputTable.setGridColor(Color.LIGHT_GRAY);
		outputTable.setModel(new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
						"Class", "Active"
				}
				) {boolean[] columnEditables = new boolean[] {false, true};
				public boolean isCellEditable(int row, int column) 
				{return columnEditables[column];}
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return columnIndex == 1 ? Boolean.class : super.getColumnClass(columnIndex);
				}
		});
		outputTable.getColumnModel().getColumn(0).setResizable(false);
		outputTable.getColumnModel().getColumn(1).setResizable(false);
		outputPane.setViewportView(outputTable);

		JLabel lblGeneratedLibraries = new JLabel("Step 3: Select libraries to generate");

		outputProgressBar = new JProgressBar();
		outputProgressBar.setStringPainted(true);

		JButton generateLibrariesBTN = new JButton("Generate Libraries");
		generateLibrariesBTN.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				try 
				{
					worker = new SwingWorker<Void, Void>()
							{

						@SuppressWarnings("unused")
						@Override
						protected Void doInBackground() throws Exception
						{
							//Run library generation
							try
							{
								String output = outputField.getText();
								//If no field given for outputField, use default directory
								if (outputField.getText().equals("")) output = "src/msp_files";
								generateLibrariesBTN.setEnabled(false);
								runLibGen(combinedOutput.isSelected(), output, (DefaultTableModel)outputTable.getModel());
								updateGenerationProgress(100,"% - Completed");
								generateLibrariesBTN.setEnabled(true);
							} 
							catch (Exception e1)
							{
								CustomError error = new CustomError("Error generating libraries", e1);
								updateGenerationProgress(100,"% - Error");
							} 

							return null;
						}

						@Override
						protected void done()
						{

						}
							};
							worker.execute();
				}
				catch (Exception e1)
				{
					CustomError error = new CustomError("Error generating libraries", e1);
					updateGenerationProgress(100,"% - Error");
				} 
			}

		});

		JButton btnSelectAll = new JButton("Select All");
		btnSelectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				try {
					selectAllOutput((DefaultTableModel)outputTable.getModel());
				} catch (Exception e1) {
					CustomError error = new CustomError(e1.getMessage(), null);
				} 
			}
		});

		JButton btnDeselectAll = new JButton("Deselect All");
		btnDeselectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				try {
					deselectAllOutput((DefaultTableModel)outputTable.getModel());
				} catch (Exception e1) {
					CustomError error = new CustomError(e1.getMessage(), null);
				} 
			}
		});

		JButton btnCheckSelected = new JButton("Check Selected");
		btnCheckSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				try {
					checkSelected(outputTable);
				} catch (Exception e1) {
					CustomError error = new CustomError(e1.getMessage(), null);
				} 
			}
		});
		GroupLayout gl_library_gen = new GroupLayout(library_gen);
		gl_library_gen.setHorizontalGroup(
			gl_library_gen.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_library_gen.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_library_gen.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(lblLibraryGeneration, GroupLayout.PREFERRED_SIZE, 98, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(475, Short.MAX_VALUE))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(lblOutputType, GroupLayout.PREFERRED_SIZE, 135, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(438, Short.MAX_VALUE))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(combinedOutput, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE)
							.addGap(2)
							.addComponent(separateOutput, GroupLayout.PREFERRED_SIZE, 135, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(327, Short.MAX_VALUE))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(outputField, GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE)
							.addGap(10)
							.addComponent(browseButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
							.addGap(13))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(lblGeneratedLibraries, GroupLayout.PREFERRED_SIZE, 194, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(379, Short.MAX_VALUE))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(outputProgressBar, GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
							.addGap(12))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(generateLibrariesBTN, GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
							.addGap(12)
							.addComponent(btnCheckSelected, GroupLayout.PREFERRED_SIZE, 114, GroupLayout.PREFERRED_SIZE)
							.addGap(10)
							.addComponent(btnDeselectAll, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
							.addGap(10)
							.addComponent(btnSelectAll, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
							.addGap(13))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(outputPane, GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
							.addGap(12))
						.addGroup(gl_library_gen.createSequentialGroup()
							.addComponent(lblOutputDirectory, GroupLayout.PREFERRED_SIZE, 381, GroupLayout.PREFERRED_SIZE)
							.addContainerGap())))
		);
		gl_library_gen.setVerticalGroup(
			gl_library_gen.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_library_gen.createSequentialGroup()
					.addGap(5)
					.addComponent(lblLibraryGeneration)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblOutputType)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_library_gen.createParallelGroup(Alignment.LEADING)
						.addComponent(combinedOutput)
						.addComponent(separateOutput))
					.addGap(16)
					.addComponent(lblOutputDirectory)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_library_gen.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_library_gen.createSequentialGroup()
							.addGap(1)
							.addComponent(outputField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addComponent(browseButton))
					.addGap(16)
					.addComponent(lblGeneratedLibraries)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(outputPane, GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)
					.addGap(18)
					.addGroup(gl_library_gen.createParallelGroup(Alignment.LEADING)
						.addComponent(generateLibrariesBTN)
						.addComponent(btnCheckSelected)
						.addComponent(btnDeselectAll)
						.addComponent(btnSelectAll))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(outputProgressBar, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		library_gen.setLayout(gl_library_gen);
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
				gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 603, Short.MAX_VALUE)
						.addGap(0))
				);
		gl_contentPane.setVerticalGroup(
				gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
						.addContainerGap())
				);
		contentPane.setLayout(gl_contentPane);
		updateOutputTable();
		setVisible(true);
	}

	//Non-GUI Methods

	//Generate library .msp files
	public static void runLibGen(boolean combined, 
			String outputDir, DefaultTableModel model) throws IOException, CustomException
	{
		ArrayList<String> selectedClasses = new ArrayList<String>();

		for (int i=0; i<model.getRowCount(); i++)
		{
			if ((boolean)model.getValueAt(i, 1))
			{
				for (int j=0; j<ms2Templates.size(); j++)
				{
					if (ms2Templates.get(j).lipidClass.name.equals(model.getValueAt(i,0)))
					{
						selectedClasses.add(ms2Templates.get(j).lipidClass.name);
					}
				}
			}
		}

		if (selectedClasses.size()>0)
		{
			//Load in all configuration files
			readFattyAcids("src/libraries/"+activeLib+"\\FattyAcids.csv");
			readAdducts("src/libraries/"+activeLib+"\\Adducts.csv");
			readClass("src/libraries/"+activeLib+"\\Lipid_Classes.csv");

			//Populate all possible FA combinations
			populateFattyAcids();
			//Populate consensusClasses
			populateConsensusClasses();

			//Read Template file
			ms2Templates = uploadTemplates(true, true, "src/libraries/"+activeLib+"\\MS2_Templates.csv");

			//Generate MS2s
			generateMS2FromTemplate(selectedClasses,transitionTypes);

			//Create MSP Files
			writeMSPFiles(combined, outputDir, selectedClasses);
		}
		//Update progress bar
		updateGenerationProgress(100,"% - Completed");
	}

	//Populate all consensus classes with class + adduct combinations
	public static void populateConsensusClasses()
	{
		consensusClasses.clear();

		//Iterate through lipidClass
		for (int i=0; i<lipidClassesDB.size(); i++)
		{
			if (outputProgressBar!= null) updateGenerationProgress((int)(Double.valueOf(i+1)
					/Double.valueOf(lipidClassesDB.size())*100.0),"% - Populating Consensus Classes");

			//Iterate through adducts
			for (int j=0; j<lipidClassesDB.get(i).adducts.length; j++)
			{
				//Create new ConsensusClass
				consensusClasses.add(new ConsensusLipidClass(lipidClassesDB.get(i),lipidClassesDB.get(i).adducts[j]));
			}
		}
	}

	//Renders all fragmentation rules in tree form
	public static DefaultTreeModel renderFragList(JTree tree,boolean addedNode) throws IOException
	{
		//Populate all possible FA combinations
		populateFattyAcids();

		//Populate consensusClasses
		populateConsensusClasses();

		//Read Template file
		if (!addedNode) ms2Templates = uploadTemplates(false, false, "src/libraries/"+activeLib+"\\MS2_Templates.csv");

		//Sort templates
		Collections.sort(ms2Templates);

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Fragmentation Library") {
			{
				DefaultMutableTreeNode node_1 = null;
				DefaultMutableTreeNode tempRule;
				//Iterate through all valid lipid class templates
				for (int i=0; i<ms2Templates.size(); i++)
				{
					node_1 = new DefaultMutableTreeNode(ms2Templates.get(i).lipidClass.getName());
					
					//Iterate through normal transitions
					for (int j=0; j<ms2Templates.get(i).transitions.size(); j++)
					{
						//Add rule node to class node
						tempRule = new DefaultMutableTreeNode(ms2Templates.get(i).transitions.get(j).displayName); 
						node_1.add(tempRule);
					}
					add(node_1);
				}
			}
		};
		
		DefaultTreeModel model = new DefaultTreeModel(root);

		return model;
	}

	//Populate all possible lipid classes in fragment rule broswer
	public static void populateFragClassList(JComboBox<String> box)
	{

		ArrayList<String> menu = new ArrayList<String>();

		for (int i=0; i<lipidClassesDB.size(); i++)
		{
			for (int j=0; j<lipidClassesDB.get(i).adducts.length; j++)
			{
				menu.add(lipidClassesDB.get(i).classAbbrev+" "+lipidClassesDB.get(i).adducts[j].name);
			}
		}

		Collections.sort(menu);

		String[] menuArray = new String[menu.size()];
		menu.toArray(menuArray);

		box.setModel(new DefaultComboBoxModel<String>(menuArray));
	}

	//Add fragmentation rule based on user inputs
	public static void addFragRule(String classString, String mass, String type, String relInt, Integer charge) throws CustomException
	{
		String templateString = "";
		int templateKey = -1;
		Double massDouble = 0.00;
		boolean formulaBoolean;

		//Look for appropriate class in classes with existing template
		for (int i=0; i<ms2Templates.size(); i++)
		{

			templateString = ms2Templates.get(i).lipidClass.name;

			if (classString.equals(templateString))
			{
				templateKey = i;
			}
		}

		//If none found, look for appropriate class in all classes and create new template
		if (templateKey<0)
		{
			for (int i=0; i<lipidClassesDB.size(); i++)
			{
				for (int j=0; j<lipidClassesDB.get(i).adducts.length; j++)
				{
					templateString = lipidClassesDB.get(i).classAbbrev+" "+lipidClassesDB.get(i).adducts[j].name;

					if (classString.equals(templateString))
					{
						ms2Templates.add(
								new MS2Template(
										new ConsensusLipidClass(lipidClassesDB.get(i),lipidClassesDB.get(i).adducts[j]),
										new ArrayList<TransitionDefinition>()));
						templateKey = ms2Templates.size()-1;
					}
				}
			}
		}

		//If no match found throw error
		if (templateKey < 0) throw new CustomException("Lipid class not found.  Make sure all tables are saved", null);

		//Check mass validity
		if (!util.isFormula(mass))
		{
			try
			{
				massDouble =  Double.parseDouble(mass);
				formulaBoolean = false;
			}
			catch (Exception e)
			{
				throw new CustomException(mass+" is not a valid mass", null);
			}
		}
		//Check formula validity
		else
		{
			try
			{
				util.validElementalFormula(mass);
				formulaBoolean = true;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				//throw new CustomException(mass+" is not a valid elemental formula");
			}
		}

		//Add fragment
		addFragRuleToTemplate(ms2Templates.get(templateKey), mass, type, relInt, charge);
	}

	//Add fragmentation rule to template
	public static void addFragRuleToTemplate(MS2Template template, String massFormula, 
			String type, String relInt, Integer charge)
	{
		TransitionDefinition transitionTemp;

		transitionTemp = new TransitionDefinition(massFormula, Double.valueOf(relInt), massFormula+","+relInt+","+charge+","+type, type, charge, getTransitionType(type));
		template.transitions.add(transitionTemp);
	}

	//Returns array list of ms2 templates based on .txt file
	public static ArrayList<MS2Template> uploadTemplates(boolean generateLipids, 
			boolean updateProgress, String filename) throws IOException
			{
		Double mass;
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
					transitionTemp = new TransitionDefinition(formulaTemp, intensity, line, split[3], Integer.valueOf(split[2]), getTransitionType(split[3]));

					transitions.add(transitionTemp);
				}

			}
		}

		reader.close();

		if (generateLipids) 
		{
			for (int i=0; i<result.size(); i++)
			{

				if (result.get(i).transitions.size() > 0)
				{
					result.get(i).generateLipids();
				}
			}
		}

		return result;
			}

	//Generate .msp file for selected classes from ms2 templates
	public static void writeMSPFiles(boolean combined, String outputDir, 
			ArrayList<String> selectedClasses) throws FileNotFoundException
	{
		String filename;
		String incompleteLibs = "";
		PrintWriter pw = null;

		outputTable.getColumnModel().getColumn(0).setResizable(false);
		outputTable.getColumnModel().getColumn(1).setResizable(false);


		//Create individual libraries
		if (!combined)
		{
			for (int i=0; i<ms2Templates.size(); i++)
			{
				updateGenerationProgress((int)(Double.valueOf(i+1)
						/Double.valueOf(ms2Templates.size())*100.0),"% - Writing Spectra");

				if (selectedClasses.contains(ms2Templates.get(i).lipidClass.name))
				{
					try
					{
						//Create filename
						filename = outputDir+"\\"+ms2Templates.get(i).lipidClass.getName()+".msp";

						try
						{
							pw = new PrintWriter(filename);
						}
						catch (IOException io)
						{
							SimpleDateFormat extendedName = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
							Date today = Calendar.getInstance().getTime();        
							String reportDate = extendedName.format(today);
							pw = new PrintWriter(outputDir+"\\"+ms2Templates.get(i).lipidClass.getName()+reportDate+".msp");
						}

						for (int j=0; j<ms2Templates.get(i).theoreticalLipids.size(); j++)
						{
							pw.print(ms2Templates.get(i).theoreticalLipids.get(j).generateMSPResult());
							pw.print("\n");
						}

						pw.close();
					}
					catch(Exception e)
					{
						incompleteLibs +=  ms2Templates.get(i).lipidClass.getName()+", ";
					}
				}
			}
		}
		//Create combined library
		else
		{
			DateFormat df = new SimpleDateFormat("yyyyMMdd");
			Date today = Calendar.getInstance().getTime();        
			String reportDate = df.format(today);
			filename = outputDir+"\\"+activeLib+"_"+reportDate+".msp";

			try
			{
				pw = new PrintWriter(filename);
			}
			catch (IOException io)
			{
				SimpleDateFormat extendedName = new SimpleDateFormat("yyyyMMddHHmmss");
				today = Calendar.getInstance().getTime();        
				reportDate = extendedName.format(today);
				pw = new PrintWriter(outputDir+"\\"+activeLib+"_"+reportDate+".msp");
			}

			for (int i=0; i<ms2Templates.size(); i++)
			{
				//Add class name to table
				try
				{
					if (selectedClasses.contains(ms2Templates.get(i).lipidClass.name))
					{
						//Create filename
						for (int j=0; j<ms2Templates.get(i).theoreticalLipids.size(); j++)
						{
							pw.print(ms2Templates.get(i).theoreticalLipids.get(j).generateMSPResult());
							pw.print("\n");
						}
					}
				}
				catch (Exception e)
				{
					CustomError ce = new CustomError("Error writing .msp", null);
				}
			}			
			pw.close();
		}

		if (!incompleteLibs.equals(""))
		{
			@SuppressWarnings("unused")
			CustomError error = new CustomError("The following libraries were not created succesfully: "
					+incompleteLibs+".  Please verify that none of the .msp files are currently open", null);
		}
	}

	//Creates an ms2 template for each lipid class
	public static void generateMS2FromTemplate(ArrayList<String> outputClasses, ArrayList<TransitionType> transitionTypes) throws CustomException
	{
		for (int i=0; i<ms2Templates.size(); i++)
		{
			if (outputClasses.contains(ms2Templates.get(i).lipidClass.name))
			{
				ms2Templates.get(i).generateInSilicoMS2(transitionTypes);
			}
			if (outputProgressBar!= null) updateGenerationProgress((int)(Double.valueOf(i+1)
					/Double.valueOf(ms2Templates.size())*100.0),"% - Generating Spectra");
		}
	}

	//Adds all active fatty acids to each lipid class
	public static void populateFattyAcids()
	{
		for (int i=0; i<lipidClassesDB.size(); i++)
		{
			lipidClassesDB.get(i).populateFattyAcids(fattyAcidsDB);

			if (outputProgressBar!= null) updateGenerationProgress((int)(Double.valueOf(i+1)
					/Double.valueOf(lipidClassesDB.size())*100.0),"% - Populating Fatty Acids");
		}
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
		fattyAcidsDB.clear();

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

		//Sort fatty acud array
		Collections.sort(fattyAcidsDB);

		//Create array from arrayList
		fattyAcidsArray = fattyAcidsDB.toArray(new FattyAcid[fattyAcidsDB.size()]);

		reader.close();

		//Create transition type objects
		transitionTypes = createTransitionTypes(faTypes);

		//Populate string array
		for (int i=0; i<transitionTypes.size(); i++)
		{
			transitionTypeStrings.add(transitionTypes.get(i).toString());
		}

		allFattyAcidTypes = faTypes;
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
		adductsDB.clear();

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
		int numFA;
		String polarity;
		LipidClass temp;
		ArrayList<String> faTypes;

		//Create file buffer
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));


		//Clear Class DB
		lipidClassesDB.clear();

		//read line if not empty
		while ((line = reader.readLine()) != null)
		{
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

				numFA = Integer.valueOf(split[7]);

				polarity = split[8];

				//Parse fatty acid types
				faTypes = new ArrayList<String>();

				for (int i=9; i<split.length; i++)
				{
					if (!split[i].equals("none"))
					{
						faTypes.add(split[i]);
					}
				}
				lipidClassesDB.add(new LipidClass(name, abbrev, head, adductArray, sterol, glycerol, sphingoid, numFA, polarity, faTypes));
			}
		}

		//Create array from arrayList
		lipidClassesArray = lipidClassesDB.toArray(new LipidClass[lipidClassesDB.size()]);

		reader.close();
	}

	//Renders fatty acids for display in table
	public static Object[][] renderFAList()
	{
		Object[][] result =  new Object[fattyAcidsArray.length][4];

		for (int i=0; i<fattyAcidsArray.length; i++)
		{
			result[i][0] = fattyAcidsArray[i].name;
			result[i][1] = fattyAcidsArray[i].type;
			result[i][2] = fattyAcidsArray[i].formula;
			result[i][3] = fattyAcidsArray[i].enabled;
		}

		return result;
	}

	//Populates all menus in fragmentation rule browser
	public static void 	populateFragEditFields (DefaultMutableTreeNode selectedNode, DefaultMutableTreeNode selectedParentNode, 
			JTextField massFormulaField, JTextField relativeIntField, JComboBox transitionTypeBTN, JComboBox classDropDown, JTextField chargeField)
	{
		String childNode = String.valueOf(selectedNode);
		String parentNode = String.valueOf(selectedParentNode);
		String[] split;
		String massFormula;
		String relInt;
		String type;
		String classString;
		String charge;

		if (!parentNode.equals("Fragmentation Library"))
		{
			//Parse text from child node
			split = childNode.split(("  +"));
			massFormula = split[0];
			relInt = split[1];
			type = split[3];
			charge = split[2];


			//Parse text from parent Node
			classString = parentNode;

			//Populate fields
			massFormulaField.setText(massFormula);
			relativeIntField.setText(relInt);
			chargeField.setText(charge);

			//Change drop down menus
			for (int i=0; i<transitionTypeBTN.getModel().getSize(); i++)
			{
				if (transitionTypeBTN.getModel().getElementAt(i).equals(type))
				{
					transitionTypeBTN.getModel().setSelectedItem(transitionTypeBTN.getModel().getElementAt(i));
				}
			}

			for (int i=0; i<classDropDown.getModel().getSize(); i++)
			{
				if (classDropDown.getModel().getElementAt(i).equals(classString))
				{
					classDropDown.getModel().setSelectedItem(classDropDown.getModel().getElementAt(i));
				}
			}

		}	
	}

	//Delete a fragmentation rule from the table
	public static void 	deleteFragRule (JTree tree)
	{
		DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		DefaultMutableTreeNode selectedParentNode = (DefaultMutableTreeNode)selectedNode.getParent();
		String childNode = String.valueOf(selectedNode);
		String parentNode = String.valueOf(selectedParentNode);

		//If a fragmentation rule
		if (!parentNode.equals("Fragmentation Library"))
		{
			//Search for correct class
			for (int i=0; i<ms2Templates.size(); i++)
			{
				if (ms2Templates.get(i).lipidClass.name.equals(parentNode))
				{
					//Search for correct transition definition
					for (int j=0; j<ms2Templates.get(i).transitions.size(); j++)
					{
						if (ms2Templates.get(i).transitions.get(j).displayName.equals(childNode))
						{
							//Delete Transition
							ms2Templates.get(i).transitions.remove(j);

							//Delete Node
							model.removeNodeFromParent(selectedNode);

							break;
						}
					}
				}
			}
		}

		//If a fragmentation class 
		if (parentNode.equals("Fragmentation Library"))
		{
			//Search for correct class
			for (int i=0; i<ms2Templates.size(); i++)
			{
				if (ms2Templates.get(i).lipidClass.name.equals(childNode))
				{
					//Delete Class
					ms2Templates.remove(i);

					//Delete Node
					model.removeNodeFromParent(selectedNode);

					break;
				}
			}
		}
	}

	//Update fragmentation rule
	@SuppressWarnings("rawtypes")
	public static void 	updateFragRule (JTree tree, DefaultMutableTreeNode selectedNode, DefaultMutableTreeNode selectedParentNode, 
			JTextField massFormulaField, JTextField relativeIntField, JComboBox transitionTypeBTN, JComboBox classDropDown, JTextField chargeField) throws NumberFormatException, CustomException
	{
		DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		String childNode = String.valueOf(selectedNode);
		String parentNode = String.valueOf(selectedParentNode);
		
		if (!parentNode.equals("Fragmentation Library"))
		{
			//Search for correct class
			for (int i=0; i<ms2Templates.size(); i++)
			{
				if (ms2Templates.get(i).lipidClass.name.equals(parentNode))
				{
					//Search for correct consensus rule
					for (int j=0; j<ms2Templates.get(i).transitions.size(); j++)
					{
						if (ms2Templates.get(i).transitions.get(j).displayName.equals(childNode))
						{
							ms2Templates.get(i).transitions.get(j).updateValues(
									Double.valueOf(relativeIntField.getText()), 
									massFormulaField.getText(), 
									String.valueOf(transitionTypeBTN.getSelectedItem().toString()), chargeField.getText());
							break;

						}
					}
				}
			}
		}
	}

	//Returns object 2d array for lipid class table
	public static Object[][] renderClassList()
	{
		Object[][] result =  new Object[lipidClassesArray.length][3];

		for (int i=0; i<lipidClassesArray.length; i++)
		{
			result[i] = lipidClassesArray[i].getTableArray();
		}

		return result;
	}

	//Returns object 2d array for adduct table
	public static Object[][] renderAdductList()
	{
		Object[][] result =  new Object[adductsArray.length][3];

		for (int i=0; i<adductsArray.length; i++)
		{
			result[i] = adductsArray[i].getTableArray();
		}

		return result;
	}

	//Resets class table to most recently saved version
	public static void resetClassTable(DefaultTableModel classTableModel)
	{
		Object[][] result = renderClassList();

		//Iterate through all results
		for (int i=0; i<result.length; i++)
		{
			//If row available for editing, reset
			if (i<classTableModel.getRowCount())
			{
				for (int j=0; j<result[i].length; j++)
				{
					classTableModel.setValueAt(result[i][j],i,j);
				}
			}
			//If there are no more rows, add them
			else
			{
				classTableModel.addRow(result[i]);
			}
		}
		//Iterate through any rows in table which are not in DB and delete
		for (int i=result.length; i<classTableModel.getRowCount(); i++)
		{
			classTableModel.removeRow(i);
			i--;
		}
	}

	//Resets adduct table to most recently saved version
	public static void resetAdductTable(DefaultTableModel adductTableModel)
	{
		Object[][] result = renderAdductList();

		//Iterate through all results
		for (int i=0; i<result.length; i++)
		{
			//If row available for editing, reset
			if (i<adductTableModel.getRowCount())
			{
				for (int j=0; j<result[i].length; j++)
				{
					adductTableModel.setValueAt(result[i][j],i,j);
				}
			}
			//If there are no more rows, add them
			else
			{
				adductTableModel.addRow(result[i]);
			}
		}
		//Iterate through any rows in table which are not in DB and delete
		for (int i=result.length; i<adductTableModel.getRowCount(); i++)
		{
			adductTableModel.removeRow(i);
			i--;
		}
	}

	//Resets fatty acid table to most recently saved version
	public static void resetFAButtons(DefaultTableModel faTableModel)
	{
		for (int i=0; i<faTableModel.getRowCount(); i++)
		{
			faTableModel.setValueAt(fattyAcidsDB.get(i).enabled, i, 3);
		}
	}

	//Updates fatty acid array table
	public static void updateFAArrays(DefaultTableModel faTableModel)
	{
		ArrayList<FattyAcid> faDBTemp = new ArrayList<FattyAcid>();
		FattyAcid[] arrayTemp = new FattyAcid[((DefaultTableModel)faTableModel).getRowCount()];

		try
		{
			if (fattyAcidTable!= null)
			{
				if (!fattyAcidTable.isEditing())
				{

					for (int i=0; i<((DefaultTableModel)faTableModel).getRowCount(); i++)
					{
						String[] row = new String[((DefaultTableModel)faTableModel).getColumnCount()];

						for (int j=0; j<((DefaultTableModel)faTableModel).getColumnCount(); j++)
						{
							row[j] = ((DefaultTableModel)faTableModel).getValueAt(i, j).toString();
						}

						faDBTemp.add(convertFAArraytoObject(row));
						arrayTemp[i] = convertFAArraytoObject(row);
					}
				}
				else
				{
					CustomError error = new CustomError("Please finish editing cell before saving table", null);
				}
			}
			else
			{
				for (int i=0; i<((DefaultTableModel)faTableModel).getRowCount(); i++)
				{
					String[] row = new String[((DefaultTableModel)faTableModel).getColumnCount()];

					for (int j=0; j<((DefaultTableModel)faTableModel).getColumnCount(); j++)
					{
						row[j] = ((DefaultTableModel)faTableModel).getValueAt(i, j).toString();
					}

					faDBTemp.add(convertFAArraytoObject(row));
					arrayTemp[i] = convertFAArraytoObject(row);
				}
			}

			fattyAcidsDB = faDBTemp;
			fattyAcidsArray = arrayTemp;
		}
		catch (CustomException e)
		{
			CustomError error = new CustomError(e.getMessage(), null);
		}
	}

	//Delete selected row from class table
	public static void deleteSelectedRows(DefaultTableModel classTableModel, JTable table)
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

	//Delete selected row from fatty acid table
	public static void deleteSelectedFARows(DefaultTableModel tableModel, JTable table)
	{
		if (tableModel.getRowCount()>0)
		{
			int row = table.getSelectedRow();
			if (row>-1)
			{
				tableModel.removeRow(row);
			}
		}
	}

	public static void writeClassArraytoCSV(String filename)
	{
		//Write file
		try {
			PrintWriter pw = new PrintWriter(filename);

			pw.println("Name,Abbreviation,HeadGroup,Adducts,Sterol,Glycerol,Sphingoid,numFattyAcids,OptimalPolarity,FA1,FA2,FA3,FA4");

			for (int i=0; i<lipidClassesDB.size(); i++)
			{
				pw.println(lipidClassesDB.get(i));
			}

			pw.close();
		} catch (FileNotFoundException e) {
			CustomException error = new CustomException("Error saving classes to .csv", null);
		}

	}

	//Write current adduct array to .csv file
	public static void writeAdductArraytoCSV(String filename)
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

	//Renders fatty acid table based on last saved data
	public static DefaultTableModel createFAModel()
	{
		DefaultTableModel model = new DefaultTableModel(renderFAList(),
				new String[] {"Name", "Type", "Formula", "Enabled"}) 
		{boolean[] columnEditables = new boolean[] {true, true, true, true};
		public boolean isCellEditable(int row, int column) 
		{return columnEditables[column];}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 3 ? Boolean.class : super.getColumnClass(columnIndex);
		}};

		updateFAArrays(model);

		return model;
	}

	//Write fatty acid array to .csv when saved
	public static void writeFAArraytoCSV(String filename)
	{
		try {
			PrintWriter pw = new PrintWriter(filename);

			pw.println("Name,Base,Formula,Enabled");

			for (int i=0; i<fattyAcidsDB.size(); i++)
			{
				pw.println(fattyAcidsDB.get(i).saveString());
			}

			pw.close();
		} catch (FileNotFoundException e) {
			CustomException error = new CustomException("Error saving fatty acids to .csv", null);
		}

	}

	//Update class array from last saved information
	public static void updateClassArrays(DefaultTableModel classTableModel, JTable table)
	{
		ArrayList<LipidClass> lipidClassesDBTemp = new ArrayList<LipidClass>();
		LipidClass[] arrayTemp = new LipidClass[classTableModel.getRowCount()];
		try
		{
			if (!table.isEditing())
			{

				for (int i=0; i<classTableModel.getRowCount(); i++)
				{
					String[] row = new String[classTableModel.getColumnCount()];

					for (int j=0; j<classTableModel.getColumnCount(); j++)
					{
						row[j] = classTableModel.getValueAt(i, j).toString();
					}

					lipidClassesDBTemp.add(convertClassArraytoObject(row));
					arrayTemp[i] = convertClassArraytoObject(row);
				}
			}
			else
			{
				CustomError error = new CustomError("Please finish editing cell before saving table", null);
			}

			lipidClassesDB = lipidClassesDBTemp;
			lipidClassesArray = arrayTemp;
		}
		catch (CustomException e)
		{
			CustomError error = new CustomError(e.getMessage(), null);
		}
	}

	//Update adduct array from last save adduct .csv
	public static void updateAdductArrays(DefaultTableModel adductTableModel, JTable table)
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

	//Convert fatty acid to name
	public static FattyAcid convertFAArraytoObject(String[] array) throws CustomException
	{
		FattyAcid result = null;
		String name;
		String formula;
		String enabled;
		String type;

		//Initialize and check all fields
		//Name
		name = array[0];
		if (!name.contains(":"))
			throw new CustomException(name+" is not a valid fatty acid name.  Please reference "
					+ "\"Shorthand Notation for Lipid Structures Derived from Mass Spectrometry\", Liebisch G. et al. 2013", null);
		else if (name.contains(":"))
		{
			try 
			{
				parseFA(name);
			}
			catch (Exception e)
			{
				throw new CustomException(name+" is not a valid name.  Please reference "
						+ "\"Shorthand Notation for Lipid Structures Derived from Mass Spectrometry\", "
						+ "Liebisch G. et al. 2013", null);
			}
		}

		//Type
		type = array[1];

		//Formula
		formula = array[2];
		if (!util.validElementalFormula(formula))
			throw new CustomException(formula+" is not a valid elemental formula", null);

		//Enabled
		enabled = array[3];

		//Initialize
		result = new FattyAcid(name, type, formula, enabled);

		return result;
	}

	//Converst array table information to adduct object
	public static Adduct convertAdductArraytoObject(String[] array) throws CustomException
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

	//Converts lipid class table information to class object
	public static LipidClass convertClassArraytoObject(String[] array) throws CustomException
	{
		LipidClass result = null;
		String className;
		String classAbbrev;
		String headGroup;
		Adduct[] adductArray;
		String[] split;
		boolean sterol = false;
		boolean glycerol = false;
		boolean sphingoid = false;
		int numFattyAcids;
		String optimalPolarity;
		ArrayList<String> fattyAcidTypes = new ArrayList<String>();

		//Name
		className = array[0];

		//Abbreviation
		classAbbrev = array[1];

		//Head Group

		headGroup = array[2];
		//Check formula
		if (!util.validElementalFormula(headGroup))
			throw new CustomException(headGroup+" is not a valid elemental formula", null);

		//Adducts
		split = array[3].split(";");
		adductArray = new Adduct[split.length];

		for (int i=0; i<split.length; i++)
		{
			boolean adductFound = false;

			for (int j=0; j<adductsDB.size(); j++)
			{
				if (adductsDB.get(j).getName().equals(split[i]))
				{
					adductArray[i] = adductsDB.get(j);
					adductFound = true;
				}
			}

			//Check adducts and throw error if necessary
			if (!adductFound) throw new CustomException(split[i]+" for class "+classAbbrev+" not found in saved adduct table", null);
		}

		//Backbone
		if (array[4].equals("Glycerol")) glycerol = true;
		else if (array[4].equals("Sterol")) sterol = true;
		else if (array[4].equals("Sphingoid")) sphingoid = true;

		//Check backbone and throw error if necessary
		if (!glycerol && !sterol && !sphingoid) throw new CustomException(array[4]
				+"is not a valid backbone choice for "+classAbbrev+".  Valid options are Glycerol, Sterol, or Sphingoid", null);

		//Num Fatty Acids
		numFattyAcids = Integer.valueOf(array[5]);
		if (numFattyAcids>4) throw new CustomException("Invalid choice for number of fatty acids for "+classAbbrev, null);

		//Optimal Polarity
		optimalPolarity = array[6];	

		//Check optimal polarity
		if (!optimalPolarity.equals("-") && !optimalPolarity.equals("+") && !optimalPolarity.equals("NA") && !optimalPolarity.equals("+/-")) 
			throw new CustomException("Invalid optimal polarity for "+classAbbrev+".  Valid options are +  ,  -  ,  +/-  ,  NA.", null);



		if (!array[7].equals("-")) fattyAcidTypes.add(array[7]);
		if (!array[8].equals("-")) fattyAcidTypes.add(array[8]);
		if (!array[9].equals("-")) fattyAcidTypes.add(array[9]);
		if (!array[10].equals("-")) fattyAcidTypes.add(array[10]);

		if (fattyAcidTypes.size() != numFattyAcids) 
			throw new CustomException("Number of fatty acids for "+classAbbrev+" does not match sn designations", null);

		//Check fatty acid array types
		for (int i=7; i<10; i++)
		{
			if (!allFattyAcidTypes.contains(array[i]) 
					&& (i-6)<=numFattyAcids)
			{
				throw new CustomException(array[i]+" for "+classAbbrev+" is not a valid choice.  "
						+ "Valid options are "+allFattyAcidTypes.toString().substring(1, allFattyAcidTypes.toString().length()-1), null);
			}
		}

		result = new LipidClass(className, classAbbrev, headGroup, 
				adductArray, sterol, glycerol, sphingoid,numFattyAcids,optimalPolarity,fattyAcidTypes);

		return result;
	}

	//Select all fatty acids from table as active
	public static void selectAll(DefaultTableModel faTableModel)
	{
		for (int i=0; i<faTableModel.getRowCount(); i++)
		{
			faTableModel.setValueAt(true, i, 3);
		}

	}

	//Deactivates all fatty acids from table
	public static void deselectAll(DefaultTableModel faTableModel)
	{
		for (int i=0; i<faTableModel.getRowCount(); i++)
		{
			faTableModel.setValueAt(false, i, 3);
		}

	}

	//Selects all possible output libraries
	public static void selectAllOutput(DefaultTableModel tableModel)
	{
		for (int i=0; i<tableModel.getRowCount(); i++)
		{
			tableModel.setValueAt(true, i, 1);
		}

	}

	//Activate all selected output libraries
	public static void checkSelected(JTable table)
	{
		int[] selected = table.getSelectedRows();

		for (int i=0; i<table.getModel().getRowCount(); i++)
		{
			for (int j=0; j<selected.length; j++)
			{
				if (selected[j]==i) table.getModel().setValueAt(true, i, 1);
			}
		}

	}

	//Deselects all ouput libraries
	public static void deselectAllOutput(DefaultTableModel tableModel)
	{
		for (int i=0; i<tableModel.getRowCount(); i++)
		{
			tableModel.setValueAt(false, i, 1);
		}

	}

	//Automatically expand all nodes in the tree
	private void expandAllNodes(JTree tree) {
		int j = tree.getRowCount();
		int i = 0;
		while(i < j) {
			tree.expandRow(i);
			i += 1;
			j = tree.getRowCount();
		}
	}

	//Update library generation status bar
	public static void updateGenerationProgress(int progress, String message)
	{
		outputProgressBar.setValue(progress);
		outputProgressBar.setString(progress + message);
		Rectangle progressRect = outputProgressBar.getBounds();
		progressRect.x = 0;
		progressRect.y = 0;
		outputProgressBar.paintImmediately(progressRect);
	}

	//Update output table when new libraries are added
	private static void updateOutputTable()
	{
		try {
			readClass("src/libraries/"+activeLib+"/Lipid_Classes.csv");
			ms2Templates = uploadTemplates(false, false, "src/libraries/"+activeLib+"\\MS2_Templates.csv");

			for (int i=0; i<ms2Templates.size(); i++)
			{
				((DefaultTableModel)outputTable.getModel()).addRow(new Object[]{ms2Templates.get(i).lipidClass.name,true});
			}

		} catch (IOException e) {
			CustomError ce = new CustomError ("Error reading potential libraries", e);
		}
	}

	//Saves fragmentation rules to .csv
	private void saveFragRules(String filename)
	{
		//Save frag results
		try
		{
			PrintWriter pw = new PrintWriter(filename);

			for (int i=0; i<ms2Templates.size(); i++)
			{
				pw.println(ms2Templates.get(i));
				pw.println("---");
			}

			pw.close();

			updateOutputTable();
		}
		catch(Exception e)
		{
			CustomError ce = new CustomError ("Error saving fragmentation rules", e);
		}
	}

	//Attempt to parse fatty acid name
	public static void parseFA(String name)
	{
		String split[] = name.split(":");
		int carbonNumber = 0;
		int dbNumber = 0;

		//remove all letter characters
		for (int i=0; i<split.length; i++)
		{
			split[i] = split[i].replaceAll("[^\\d.]", "");
			split[i] = split[i].replaceAll("-", "");
		}


		//Find carbon number
		carbonNumber = Integer.valueOf(split[0]);

		//Find db number
		dbNumber = Integer.valueOf(split[1]);
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
