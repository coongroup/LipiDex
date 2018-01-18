package peak_finder;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.ImageIcon;

import compound_discoverer.CDPeakFinder;
import lib_gen.Adduct;
import lib_gen.CustomError;
import lib_gen.CustomException;
import mzmine.MzPeakFinder;
import javax.swing.SwingConstants;



@SuppressWarnings("serial")
public class PeakFinderGUI extends JInternalFrame {

	private JPanel contentPane;										//Main content pane
	private JTextField firstFilePath;								//First filepath textbox
	private JTextField secondFilePath;								//Second filepath textbox
	public JProgressBar progressBar = null;							//Progress bar
	private JTable ms2ResultTable;									//Tabel for all ms2 result files for analysis
	public CDPeakFinder cdPeakFinder;								//Compound Discoverer Peak Finder
	public MzPeakFinder mzPeakFinder;								//MzMine 2 Peak Finder
	private JButton alignedTableButton;								//Add button for first text box
	private JButton unalignedTableButton;							//Add button for second text box
	private JLabel secondBoxLabel;									//Label for second filepath box	
	private JLabel firstBoxLabel;									//Label for first filepath box
	private JButton btnAdd;											//Add button
	private JButton btnClear;										//Clear button
	private JButton btnRun;											//Run Button
	private JButton btnConfig;										//Adduct filtering config
	private JCheckBox chckbxUnidentifiedFeatureFiltering;			//Adduct filtering checkbox
	private JCheckBox chckbxInsourceFragmentFiltering;				//In-source fragment filtering checkbox
	private SwingWorker<Void, Void> worker;							//Swingworker for handing PeakFinder algorithms
	public boolean separatePolarities = false;						//True iff raw files were collected in separate polarities
	public boolean[] separatePolaritiesArray = new boolean[1];		//Array of polarity ids
	public File lastDirectory = new File("C:");						//Last directory accessed
	int progressInt = 0;											//Current progress
	static ArrayList<Adduct> adductsDB = new ArrayList<Adduct>();	//ArrayList of all adducts from active lib

	//Launch Peak Finder
	public static void main(JDesktopPane contentPane, JLabel label, ImageIcon onImage, ImageIcon offImage) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PeakFinderGUI frame = new PeakFinderGUI(contentPane, label, onImage, offImage);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();

				}
			}
		});
	}

	//Constructor
	@SuppressWarnings({ "unused", "unchecked", "rawtypes" })
	public PeakFinderGUI(JDesktopPane desktopPane, JLabel label, ImageIcon onImage, ImageIcon offImage) 
	{
		setFrameIcon(new ImageIcon("src/icons/pf_16_icon.png"));
		File cdFile = null;
		try {
			readAdducts("src/peak_finder/Possible_Adducts.csv");
		} catch (IOException e3) {
			CustomError ce = new CustomError ("Error reading adduct table", e3);
		}

		//Change menu icon when closed
		addInternalFrameListener(new InternalFrameAdapter()
		{
			public void internalFrameClosing(InternalFrameEvent e) 
			{
				label.setIcon(offImage);
			}
		});

		//Change menu icon when opened
		addInternalFrameListener(new InternalFrameAdapter()
		{
			public void internalFrameOpened(InternalFrameEvent e) 
			{
				label.setIcon(onImage);
			}
		});


		//Initialize GUI parameters
		try {
			this.setSelected(true);
		} catch (PropertyVetoException e2) {
			e2.printStackTrace();
		}

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}

		File rawFile = null;
		setClosable(true);
		this.setIconifiable(true);
		setTitle("Peak Finder");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(25, 25, 579, 685);
		contentPane = new JPanel();
		contentPane.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the minimum number of times/n the specific"
				+ " feature was identified to be included in the final peak table."+"</p></html>");
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		//Create list object
		DefaultListModel<String> model = new DefaultListModel();
		boolean rtFilter = true;

		firstFilePath = new JTextField();
		firstFilePath.setEnabled(false);
		firstFilePath.setBounds(10, 100, 449, 20);
		contentPane.add(firstFilePath);
		firstFilePath.setColumns(10);

		firstBoxLabel = new JLabel("");
		firstBoxLabel.setBounds(10, 85, 449, 14);
		contentPane.add(firstBoxLabel);

		alignedTableButton = new JButton("Add");
		alignedTableButton.setEnabled(false);
		alignedTableButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						".csv files", "csv");
				chooser.setFileFilter(filter);
				chooser.setCurrentDirectory(lastDirectory);
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					firstFilePath.setText(chooser.getSelectedFile().getAbsolutePath());
					lastDirectory = new File(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		alignedTableButton.setBounds(464, 98, 89, 23);
		contentPane.add(alignedTableButton);

		JLabel lblParamaters = new JLabel("MS/MS Filtering Parameters");
		lblParamaters.setBounds(10, 363, 144, 14);
		contentPane.add(lblParamaters);

		JLabel lblMinMassDifference = new JLabel("Max. Mass Difference (ppm)");
		lblMinMassDifference.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the maximum relative mass difference (ppm)"
				+ " allowed to associate a lipid identification with a chromatographic peak."+"</p></html>");
		lblMinMassDifference.setBounds(329, 446, 144, 14);
		contentPane.add(lblMinMassDifference);

		JLabel label_2 = new JLabel("Min. MS2 Search Dot Product");
		label_2.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the minimum spectral"
				+ " similarity score needed to use an MS/MS identification."+"</p></html>");
		label_2.setBounds(10, 418, 144, 14);
		contentPane.add(label_2);

		JLabel label_3 = new JLabel("Min. MS2 Search Rev. Dot Product");
		label_3.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the minimum"
				+ " reverse spectral similarity score needed to use an MS/MS identification."+"</p></html>");
		label_3.setBounds(10, 443, 174, 14);
		contentPane.add(label_3);

		JLabel lblMinLipidSpectral = new JLabel("Min. Lipid Spectral Purity (%)");
		lblMinLipidSpectral.setToolTipText("<html><p width=\"500\">"+"This parameter specifies"
				+ " the minimum spectral purity needed "+"</p></html>");
		lblMinLipidSpectral.setBounds(10, 393, 139, 14);
		contentPane.add(lblMinLipidSpectral);

		JSpinner PPM_Diff = new JSpinner();
		PPM_Diff.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the maximum relative mass "
				+ "difference (ppm) allowed to associate a lipid identification with a chromatographic peak."+"</p></html>");
		PPM_Diff.setBounds(507, 443, 46, 20);
		contentPane.add(PPM_Diff);
		PPM_Diff.setModel(new SpinnerNumberModel(15, 0, 100, 1));

		JSpinner MS2_DP = new JSpinner();
		MS2_DP.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the minimum spectral "
				+ "similarity score needed to use an MS/MS identification."+"</p></html>");
		MS2_DP.setBounds(190, 415, 46, 20);
		contentPane.add(MS2_DP);
		MS2_DP.setModel(new SpinnerNumberModel(500, 0, 1000, 1));

		JSpinner MS2_Rev_DP = new JSpinner();
		MS2_Rev_DP.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the minimum reverse"
				+ " spectral similarity score needed to use an MS/MS identification."+"</p></html>");
		MS2_Rev_DP.setBounds(190, 440, 46, 20);
		contentPane.add(MS2_Rev_DP);
		MS2_Rev_DP.setModel(new SpinnerNumberModel(700, 0, 1000, 1));

		JSpinner Purity = new JSpinner();
		Purity.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the minimum spectral purity needed"
				+ " \r\nto annotate a lipid at the molecular composition level (PC 16:1_18:1) rather than at the sum composition level (PC 34:2)."+"</p></html>");
		Purity.setBounds(190, 390, 46, 20);
		contentPane.add(Purity);
		Purity.setModel(new SpinnerNumberModel(75, 0, 100, 1));

		JLabel label_4 = new JLabel("Feature Association Parameters");
		label_4.setBounds(331, 391, 172, 14);
		contentPane.add(label_4);

		JLabel label_5 = new JLabel("FWHM Window Multiplier");
		label_5.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the maximum "
				+ "allowed retention difference between the apex of the chromatographic peak and the MS/MS spectra in terms of the FWHM of the chromatographic peak."+"</p></html>");
		label_5.setBounds(329, 421, 125, 14);
		contentPane.add(label_5);

		JSpinner FWHM = new JSpinner();
		FWHM.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the maximum allowed "
				+ "retention difference between the apex of the chromatographic peak and the MS/MS spectra in terms of the FWHM of the chromatographic peak."+"</p></html>");
		FWHM.setBounds(507, 418, 46, 20);
		contentPane.add(FWHM);
		FWHM.setModel(new SpinnerListModel(new String[] {"0.0", "0.1", "0.2", "0.3", "0.4", "0.5", 
				"0.6", "0.7", "0.8", "0.9", "1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7",
				"1.8", "1.9", "2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", 
				"3.0","3.4","3.8","4.2","4.6","5.0"}));
		FWHM.setValue("2.0");

		progressBar = new JProgressBar();
		progressBar.setBounds(10, 616, 543, 26);
		contentPane.add(progressBar);
		progressBar.setToolTipText("");
		progressBar.setStringPainted(true);

		JSpinner rtFilterSpinner = new JSpinner();
		rtFilterSpinner.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the maximum allowed"
				+ " retention time difference between a lipid identification and the all other identified lipids "
				+ "of the same class in terms of multiples of the median absolute retention time deviation of the lipid class."+"</p></html>");
		rtFilterSpinner.setModel(new SpinnerListModel(new String[] {"1.0", "1.1", "1.2", "1.3", 
				"1.4", "1.5", "1.6", "1.7", "1.8", "1.9", "2.0", "2.1", "2.2", "2.3", "2.4", 
				"2.5", "2.6", "2.7", "2.8", "2.9", "3.0", "3.1", "3.2", "3.3", "3.4", "3.5", 
				"3.6", "3.7", "3.8", "3.9", "4.0"}));
		rtFilterSpinner.setBounds(507, 525, 46, 20);
		contentPane.add(rtFilterSpinner);
		rtFilterSpinner.setEnabled(rtFilter);
		rtFilterSpinner.setValue("3.5");

		JCheckBox rtfilteringbox = new JCheckBox("Max. RT M.A.D Factor");
		rtfilteringbox.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the maximum allowed"
				+ " retention time difference between a lipid identification and the all other identified lipids"
				+ " of the same class in terms of multiples of the median absolute retention time deviation of the lipid class."+"</p></html>");
		rtfilteringbox.setBounds(326, 524, 144, 23);
		contentPane.add(rtfilteringbox);
		rtfilteringbox.setSelected(rtFilter);

		JRadioButton cdRadioButton = new JRadioButton("Compound Discoverer");
		cdRadioButton.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				setLabelText("CD");
				alignedTableButton.setEnabled(true);
				unalignedTableButton.setEnabled(true);
				btnRun.setEnabled(true);
			}
		});
		cdRadioButton.setBounds(69, 30, 131, 23);
		contentPane.add(cdRadioButton);

		JRadioButton mzmineRadioButton = new JRadioButton("MZmine 2");
		mzmineRadioButton.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				setLabelText("MZ");
				alignedTableButton.setEnabled(true);
				unalignedTableButton.setEnabled(true);
				btnRun.setEnabled(true);
			}
		});
		mzmineRadioButton.setBounds(202, 30, 109, 23);
		contentPane.add(mzmineRadioButton);

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(cdRadioButton);
		buttonGroup.add(mzmineRadioButton);

		JLabel lblFileType = new JLabel("File Type:");
		lblFileType.setBounds(10, 34, 53, 14);
		contentPane.add(lblFileType);

		JScrollPane ms2ResultScrollPane = new JScrollPane();
		ms2ResultScrollPane.setBounds(10, 203, 543, 143);
		contentPane.add(ms2ResultScrollPane);

		ms2ResultTable = new JTable();
		DefaultTableModel ms2ResultTableModel = new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
						"File"
				}
				) {
			Class[] columnTypes = new Class[] {
					String.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		};
		ms2ResultTable.setModel(ms2ResultTableModel);
		ms2ResultTable.getColumnModel().getColumn(0).setResizable(false);

		ms2ResultScrollPane.setViewportView(ms2ResultTable);

		JSeparator separator = new JSeparator();
		separator.setBounds(10, 380, 224, 2);
		contentPane.add(separator);

		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(329, 408, 224, 2);
		contentPane.add(separator_1);

		btnClear = new JButton("Clear");
		btnClear.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				((DefaultTableModel)ms2ResultTable.getModel()).setRowCount(0);
			}
		});
		btnClear.setBounds(464, 357, 89, 23);
		contentPane.add(btnClear);

		btnAdd = new JButton("Load Files");
		btnAdd.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try {
					FileBrowser fb = new FileBrowser(ms2ResultTable, separatePolaritiesArray, lastDirectory);
					fb.toFront();
					fb.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
					fb.setVisible(true);
					desktopPane.add(fb);
					fb.toFront();
				} catch (Exception e) {
					CustomError ce = new CustomError("Error loading file browser", e);
				}
			}
		});
		btnAdd.setBounds(365, 357, 89, 23);
		contentPane.add(btnAdd);

		secondFilePath = new JTextField();
		secondFilePath.setEnabled(false);
		secondFilePath.setColumns(10);
		secondFilePath.setBounds(10, 146, 449, 20);
		contentPane.add(secondFilePath);

		secondBoxLabel = new JLabel("");
		secondBoxLabel.setBounds(10, 130, 441, 14);
		contentPane.add(secondBoxLabel);

		unalignedTableButton = new JButton("Add");
		unalignedTableButton.setEnabled(false);
		unalignedTableButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						".csv files", "csv");
				chooser.setFileFilter(filter);
				chooser.setCurrentDirectory(lastDirectory);
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					secondFilePath.setText(chooser.getSelectedFile().getAbsolutePath());
					lastDirectory = new File(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		unalignedTableButton.setBounds(464, 144, 89, 23);
		contentPane.add(unalignedTableButton);

		JLabel lblFeatureIdentifiedIn = new JLabel("Feature Found\r\n in n Files");
		lblFeatureIdentifiedIn.setBounds(329, 553, 144, 14);
		contentPane.add(lblFeatureIdentifiedIn);

		JSpinner featureNumberSpinner = new JSpinner();
		featureNumberSpinner.setToolTipText("<html><p width=\"500\">"+"This parameter specifies the minimum number of"
				+ " times the specific feature was identified to be included in the final peak table."+"</p></html>");
		featureNumberSpinner.setModel(new SpinnerNumberModel(new Integer(2), new Integer(1), null, new Integer(1)));
		featureNumberSpinner.setBounds(507, 550, 46, 20);
		contentPane.add(featureNumberSpinner);

		//RTFilter action
		rtfilteringbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				updateRTSpinner(rtFilterSpinner);
			}
		});

		btnRun = new JButton("Identify Chromatographic Peaks");
		btnRun.setEnabled(false);
		btnRun.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent arg0) 
			{
				//Change run button
				btnRun.setEnabled(false);
				Rectangle btnNewButton_2Rect = btnRun.getBounds();
				btnNewButton_2Rect.x = 0;
				btnNewButton_2Rect.y = 0;
				btnRun.paintImmediately(btnNewButton_2Rect);

				//Update utilities parameters
				Utilities.MINDOTPRODUCT = Double.valueOf(String.valueOf(MS2_DP.getValue()));
				Utilities.MINFAPURITY = Double.valueOf(String.valueOf(Purity.getValue()));
				Utilities.MAXPPMDIFF = Double.valueOf(String.valueOf(PPM_Diff.getValue()));
				Utilities.MINREVDOTPRODUCT = Double.valueOf(String.valueOf(MS2_Rev_DP.getValue()));
				Utilities.MINRTMULTIPLIER = Double.valueOf(String.valueOf(FWHM.getValue()));
				ArrayList<File> idFiles = new ArrayList<File>();
				ArrayList<Integer> samplePairNumbers = new ArrayList<Integer>();
				File cdFile = new File(firstFilePath.getText());
				File featureFile = new File(secondFilePath.getText());

				try
				{
				
				//Detect if separate polarities used
				if (ms2ResultTable.getModel().getColumnCount()>1) separatePolarities = true;

				//Check result files exist
				for (int i=0; i<ms2ResultTable.getRowCount(); i++)
				{
					if (separatePolarities) samplePairNumbers.add((Integer)ms2ResultTable.getValueAt(i, 1));

					try
					{
						File rawFile = new File((String)ms2ResultTable.getValueAt(i, 0));
						if (rawFile.exists()) idFiles.add(rawFile);
					}
					catch (Exception e)
					{
						Error e1 = new Error("Error loading file"+(String)ms2ResultTable.getValueAt(i, 0));
					}
				}
				}
				catch (Exception e)
				{
					Error e1 = new Error("Error reading table.  Please restart LipiDex",e);
				}

				//If compound discover and all files are valid, run quantitation
				if (cdFile.exists() && featureFile.exists() && cdRadioButton.isSelected())
					startCDQuantitation(progressBar, featureNumberSpinner, rtfilteringbox, 
							idFiles, rtFilterSpinner, btnRun, separatePolaritiesArray[0], samplePairNumbers);

				//If mzmine and all files are valid, run quantitation
				else if(cdFile.exists() && mzmineRadioButton.isSelected())
					startMzMineQuantitation(progressBar, featureNumberSpinner, rtfilteringbox, idFiles,
							rtFilterSpinner, btnRun, separatePolaritiesArray[0], samplePairNumbers);
				//If mzmine and all files are valid, run quantitation
				else if(featureFile.exists() && mzmineRadioButton.isSelected())
					startMzMineQuantitation(progressBar, featureNumberSpinner, rtfilteringbox, idFiles,
							rtFilterSpinner, btnRun, separatePolaritiesArray[0], samplePairNumbers);

				//Else throw error
				else
				{
					CustomError e = new CustomError (cdFile.getName()+" does not exist", null);
					btnRun.setEnabled(true);
				}
			}});
		btnRun.setBounds(10, 582, 543, 23);
		contentPane.add(btnRun);

		JSeparator separator_2 = new JSeparator();
		separator_2.setBounds(10, 26, 543, 2);
		contentPane.add(separator_2);

		JLabel lblStepSelect = new JLabel("Step 1: Select peak table type");
		lblStepSelect.setBounds(10, 11, 174, 14);
		contentPane.add(lblStepSelect);

		JLabel lblStepUpload = new JLabel("Step 2: Upload peak tables");
		lblStepUpload.setBounds(10, 60, 174, 14);
		contentPane.add(lblStepUpload);

		JSeparator separator_3 = new JSeparator();
		separator_3.setBounds(10, 75, 543, 2);
		contentPane.add(separator_3);

		JLabel lblStepUpload_1 = new JLabel("Step 3: Upload MS/MS result files");
		lblStepUpload_1.setBounds(10, 177, 174, 14);
		contentPane.add(lblStepUpload_1);

		JSeparator separator_4 = new JSeparator();
		separator_4.setBounds(10, 192, 543, 2);
		contentPane.add(separator_4);

		chckbxUnidentifiedFeatureFiltering = new JCheckBox("Adduct/Dimer Filtering");
		chckbxUnidentifiedFeatureFiltering.setSelected(true);
		chckbxUnidentifiedFeatureFiltering.setBounds(8, 524, 131, 23);
		contentPane.add(chckbxUnidentifiedFeatureFiltering);

		JLabel lblUnknownFilteringParameters = new JLabel("Result Filtering Parameters");
		lblUnknownFilteringParameters.setBounds(12, 503, 144, 14);
		contentPane.add(lblUnknownFilteringParameters);

		JSeparator separator_5 = new JSeparator();
		separator_5.setBounds(12, 520, 541, 2);
		contentPane.add(separator_5);

		chckbxInsourceFragmentFiltering = new JCheckBox("In-source Fragment Filtering");
		chckbxInsourceFragmentFiltering.setSelected(true);
		chckbxInsourceFragmentFiltering.setBounds(8, 552, 161, 23);
		contentPane.add(chckbxInsourceFragmentFiltering);

		btnConfig = new JButton("Configure");
		btnConfig.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent arg0) 
			{
				try {
					AdductWindow aw = new AdductWindow(adductsDB);
					aw.toFront();
					aw.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
					aw.setVisible(true);
					desktopPane.add(aw);
					aw.toFront();
				} catch (Exception e) {
					CustomError ce = new CustomError("Error reading adduct table", e);
				}
			}
		});
		btnConfig.setBounds(143, 524, 89, 23);
		contentPane.add(btnConfig);
	}

	//Inactivates buttons when PeakFinder is running
	private void setButtonStatus(Boolean status)
	{
		alignedTableButton.setEnabled(status);
		unalignedTableButton.setEnabled(status);
		btnAdd.setEnabled(status);
		btnClear.setEnabled(status);
		btnRun.setEnabled(status);
		btnConfig.setEnabled(status);
	}

	//Sets box label text when changing peak table type
	private void setLabelText(String type)
	{
		firstFilePath.setEnabled(true);
		secondFilePath.setEnabled(true);

		if (type.equals("CD"))
		{
			firstBoxLabel.setText("Aligned Peak Table (.csv)");
			secondBoxLabel.setText("Unaligned Peak Table (.csv)");
		}
		if (type.equals("MZ"))
		{
			firstBoxLabel.setText("Positive Polarity Peak Table (.csv)");
			secondBoxLabel.setText("Negative Polarity Peak Table (.csv)");
		}
	}

	//Runs compound discoverer quantitation
	private void startCDQuantitation(JProgressBar progressBar, JSpinner featureNumberSpinner, 
			JCheckBox rtfilteringbox,ArrayList<File> idFiles, JSpinner rtFilterSpinner, 
			JButton btnRun, boolean separatePolarities, ArrayList<Integer> samplePairNumbers)
	{
		worker = new SwingWorker<Void, Void>() 
				{
			@SuppressWarnings("unused")
			@Override
			protected Void doInBackground() throws Exception
			{
				setButtonStatus(false);
				//Run Quant
				try
				{
					cdPeakFinder = new CDPeakFinder(firstFilePath.getText(),secondFilePath.getText(), 
							idFiles, Integer.valueOf(String.valueOf(featureNumberSpinner.getValue()))
							,rtfilteringbox.isEnabled(),Double.valueOf(String.valueOf(
									rtFilterSpinner.getValue())),progressBar, samplePairNumbers, adductsDB);
					cdPeakFinder.runQuantitation(separatePolarities, chckbxUnidentifiedFeatureFiltering.isSelected(),
							chckbxInsourceFragmentFiltering.isSelected());
				}
				catch(CustomException e)
				{
					CustomError er = new CustomError(e.getMessage(), e.getException());
				}
				catch(Exception e)
				{
					CustomError er = new CustomError(e.getMessage(), e);
				}

				return null;
			}

			@Override
			protected void done()
			{
				setButtonStatus(true);
				updateProgress(1, 100, "% - Completed");
			}
				};		

				worker.execute();
	}

	//Runs mzmine quantitation
	private void startMzMineQuantitation(JProgressBar progressBar, JSpinner featureNumberSpinner, JCheckBox rtfilteringbox,
			ArrayList<File> idFiles, JSpinner rtFilterSpinner, JButton btnRun, boolean separatePolarities, ArrayList<Integer> samplePairNumbers)
	{
		worker = new SwingWorker<Void, Void>() 
				{
			@Override
			protected Void doInBackground() throws Exception
			{
				setButtonStatus(false);
				//Run Quant
				try
				{
					mzPeakFinder = new MzPeakFinder(firstFilePath.getText(),secondFilePath.getText(), idFiles, Integer.valueOf(String.valueOf(featureNumberSpinner.getValue()))
							,rtfilteringbox.isEnabled(),Double.valueOf(String.valueOf(rtFilterSpinner.getValue())),progressBar, samplePairNumbers, adductsDB);
					mzPeakFinder.runQuantitation(separatePolarities, chckbxUnidentifiedFeatureFiltering.isSelected(),
							chckbxInsourceFragmentFiltering.isSelected());
				}
				catch(Exception e)
				{
					CustomError ce = new CustomError(e.getMessage(), e);
				}

				return null;
			}

			@Override
			protected void done()
			{
				setButtonStatus(true);
				updateProgress(1, 100, "% - Completed");
			}
				};		

				worker.execute();
	}

	//Updates progress bar
	public void updateProgress(int bar, int progress, String message)
	{
		if (progress != progressInt)
		{
			progressBar.setValue(progress);
			progressBar.setString(progress+message);
			progressBar.repaint();
		}
	}

	//Updates retention time spinner
	public void updateRTSpinner(JSpinner spinner)
	{
		if (spinner.isEnabled()) spinner.setEnabled(false);
		else spinner.setEnabled(true);
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

		reader.close();
	}
}

