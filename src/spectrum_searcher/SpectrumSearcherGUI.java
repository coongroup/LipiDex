package spectrum_searcher;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JInternalFrame;
import javax.swing.ImageIcon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lib_gen.CustomException;
import lib_gen.CustomError;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class SpectrumSearcherGUI extends JInternalFrame {
	public JTable availableLibsTable;
	public JScrollPane availableLibsScroll;
	private ArrayList<File> lipidLibraries = new ArrayList<File>();
	private ArrayList<File> selectedLipidLibraries = new ArrayList<File>();
	private ArrayList<File> mgfFiles = new ArrayList<File>();
	private String[][] lipidLibNames;
	private JTextField lowMassBox;
	private SwingWorker<Void, Void> worker;
	private File lastDirectory = new File("C:");
	private JSpinner maxSearchResultsSpinner;
	private JTextField ms2TolBox;
	private JTextField ms1TolBox;
	private JList filesList;
	private JButton searchSpectraButton;
	private JButton selectAllButton;
	private JButton deselectAllButton;
	private JButton btnAdd;
	private JButton btnClear;

	//Launch Spectrum Searcher
	public static void main(JLabel label, ImageIcon onImage, ImageIcon offImage) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SpectrumSearcherGUI frame = new SpectrumSearcherGUI(label, onImage, offImage);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//Constructor
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public SpectrumSearcherGUI(JLabel label, ImageIcon onImage, ImageIcon offImage) throws IOException {

		//Load all active libraries
		loadLibraries("src/msp_files");

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


		try {
			this.setSelected(true);
		} catch (PropertyVetoException e2) {
			CustomError ce = new CustomError("Error", e2); 
		}

		//Initialize GUI
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}
		setFrameIcon(new ImageIcon(SpectrumSearcherGUI.class.getResource("/icons/ss_16_icon.png")));
		setTitle("Spectrum Searcher");
		setBounds(100, 100, 590, 437);
		setMinimumSize(new Dimension(585, 325));
		setClosable(true);
		this.setIconifiable(true);
		this.setResizable(true);
		JScrollPane filesScrollPane = new JScrollPane();
		JProgressBar progressBar = new JProgressBar();
		progressBar.setStringPainted(true);

		maxSearchResultsSpinner = new JSpinner();
		maxSearchResultsSpinner.setToolTipText("<html><p width=\"500\">"+"This field denotes the maximum possible number of"
				+ " search results returned for each spectrum"+"</p></html>");
		maxSearchResultsSpinner.setModel(new SpinnerNumberModel(1, 1, 30, 1));

		ms2TolBox = new JTextField();
		ms2TolBox.setToolTipText("<html><p width=\"500\">"+"This field denotes the absolute mass tolerance used for spectral similarity "
				+ "scoring.  An entry of 0.01 denotes a mass tolerance of +/- 0.01 Th"+"</p></html>");
		ms2TolBox.setText("0.01");
		ms2TolBox.setHorizontalAlignment(SwingConstants.TRAILING);

		DefaultListModel<String> filesListModel = new DefaultListModel();
		filesList = new JList(filesListModel);
		filesScrollPane.setViewportView(filesList);

		ms1TolBox = new JTextField();
		ms1TolBox.setToolTipText("<html><p width=\"500\">"+"This field denotes the absolute mass tolerance used to search for valid lipids "
				+ "for the precursor of each MS/MS spectrum.  An entry of 0.01 denotes a +/- 0.01 Th window."+"</p></html>");
		ms1TolBox.setHorizontalAlignment(SwingConstants.TRAILING);
		ms1TolBox.setText("0.01");
		JLabel lblMsSearchTolerance = new JLabel("MS1 Search Tolerance (Th)");
		lblMsSearchTolerance.setToolTipText("<html><p width=\"500\">"+"This field denotes the absolute mass tolerance used to "
				+ "search for valid lipids for the precursor of each MS/MS spectrum.  An entry of 0.01 denotes a +/- 0.01 Th window."+"</p></html>");

		JLabel lblMsSearchTolerance_1 = new JLabel("MS2 Search Tolerance (Th)");
		lblMsSearchTolerance_1.setToolTipText("<html><p width=\"500\">"+"This field denotes the absolute mass tolerance used "
				+ "for spectral similarity scoring.  An entry of 0.01 denotes a mass tolerance of +/- 0.01 Th"+"</p></html>");

		JLabel lblSearchResultsReturned = new JLabel("Max Search Results Returned");
		lblSearchResultsReturned.setToolTipText("<html><p width=\"500\">"+"This field denotes the maximum possible number of "
				+ "search results returned for each spectrum"+"</p></html>");

		availableLibsScroll = new JScrollPane();

		lowMassBox = new JTextField();
		lowMassBox.setToolTipText("<html><p width=\"500\">"+"This field denotes the minimum m/z value in each ms/ms spectrum"
				+ " which will be used for spectral similarity scoring. "+"</p></html>");
		lowMassBox.setText("61.00");
		lowMassBox.setHorizontalAlignment(SwingConstants.TRAILING);

		JLabel lowMassCutoff = new JLabel("MS2 Low Mass Cutoff (Th)");
		lowMassCutoff.setToolTipText("<html><p width=\"500\">"+"This field denotes the minimum m/z value in each ms/ms "
				+ "spectrum which will be used for spectral similarity scoring. "+"</p></html>");

		availableLibsTable = new JTable();
		availableLibsTable.setModel(new DefaultTableModel(
				lipidLibNames,
				new String[] {
						"Library", "Enabled"
				})
		{boolean[] columnEditables = new boolean[] {false, true};
		public boolean isCellEditable(int row, int column) 
		{return columnEditables[column];}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 1 ? Boolean.class : super.getColumnClass(columnIndex);
		}});		

		availableLibsTable.getColumnModel().getColumn(1).setResizable(false);
		availableLibsTable.getColumnModel().getColumn(1).setMinWidth(75);
		availableLibsTable.getColumnModel().getColumn(1).setMaxWidth(75);
		availableLibsScroll.setViewportView(availableLibsTable);

		searchSpectraButton = new JButton("Search Spectra");
		searchSpectraButton.setEnabled(false);
		searchSpectraButton.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				ArrayList<String> mgfFiles = new ArrayList<String>();
				ArrayList<String> mzXMLFiles = new ArrayList<String>();
				boolean isLipidBlast = false;

				try
				{
					//Check validity of numbers supplied
					Double.valueOf(ms1TolBox.getText());
					Double.valueOf(ms2TolBox.getText());
				}
				catch (Exception er)
				{
					CustomError e1 = new CustomError("Mass tolerance value not a valid number", null);
				}

				try
				{
					//Clear selected lipid libraries
					selectedLipidLibraries = new ArrayList<File>();
					
					//Load all selected libraries
					for (int i=0; i<availableLibsTable.getModel().getRowCount(); i++)
					{
						if (Boolean.valueOf(String.valueOf(availableLibsTable.getModel().getValueAt(i, 1))))
							selectedLipidLibraries.add(lipidLibraries.get(i));
					}

					for (int i=0; i<filesListModel.getSize(); i++)
					{
						if (filesListModel.getElementAt(i).endsWith(".mgf")) mgfFiles.add(filesListModel.getElementAt(i));
						else if (filesListModel.getElementAt(i).endsWith(".mzXML")) mzXMLFiles.add(filesListModel.getElementAt(i));				
					}

					//Check if any libraries were selected
					if (selectedLipidLibraries.size() == 0) throw new CustomException("No libraries selected", null);

					//Search spectra using swing worker
					startSearching(mzXMLFiles, mgfFiles,  progressBar, ms1TolBox, ms2TolBox, maxSearchResultsSpinner);
					
					//If lipidBlast used, pop up citation window
					for (int i=0; i<selectedLipidLibraries.size(); i++)
					{
						if (selectedLipidLibraries.get(i).getName().contains("LipidBlast"))
						{
							isLipidBlast = true;
						}
					}
					
					if (isLipidBlast)
					{
						CustomMessage cm = new CustomMessage ("The use of LipidBlast falls under the Creative-Commons By-Attribution (CC-BY) license."
								+ "  If used, you must correctly cite the following publications."
								+ "\n\nKind T, Liu KH, Lee do Y, DeFelice B, "
								+ "Meissen JK, Fiehn O. LipidBlast in silico tandem mass spectrometry database "
								+ "for lipid identification. Nature Methods. 2013 Aug;10(8):755-8. "
								+ "doi: 10.1038/nmeth.2551. Epub 2013 Jun 30.\n\n"
								+ "Tsugawa H, Ikeda K, Tanaka W, Senoo Y, Arita M, Arita. "
								+ "Comprehensive identification of sphinglipid species by in silico "
								+ "retention time and tandem mass spectral library. J. Cheminform. 2017 Mar;9(19). "
								+ "doi: 10.1186/s13321-017-0205-3.");
					}
				}
				catch(CustomException er)
				{
					CustomError e1 = new CustomError(er.getMessage(), null);
				}
				catch(Exception er)
				{
					CustomError e1 = new CustomError(er.getMessage(), er);
				}

			}
		});

		selectAllButton = new JButton("Select All");
		selectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				selectAll((DefaultTableModel)availableLibsTable.getModel());
			}
		});

		deselectAllButton = new JButton("Deselect All");
		deselectAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				deselectAll((DefaultTableModel)availableLibsTable.getModel());
			}
		});

		JLabel lblNewLabel = new JLabel("Available Libraries (.msp)");

		JLabel inputFilesLabel = new JLabel("Input Files (mgf,mzXML)");

		JSeparator separator = new JSeparator();

		JLabel lblNewLabel_2 = new JLabel("MS/MS Search Parameters");

		btnAdd = new JButton("Add");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Create Array List
					File[] rawFiles;

					JFileChooser chooser = new JFileChooser();
					FileNameExtensionFilter filterMGF = new FileNameExtensionFilter(
							".mgf files", "mgf");
					FileNameExtensionFilter filterMZXML = new FileNameExtensionFilter(
							".mzXML files", "mzXML");
					chooser.setMultiSelectionEnabled(true);
					chooser.addChoosableFileFilter(filterMGF);
					chooser.addChoosableFileFilter(filterMZXML);
					chooser.setCurrentDirectory(lastDirectory);
					int returnVal = chooser.showOpenDialog(null);
					if(returnVal == JFileChooser.APPROVE_OPTION) 
					{
						//Add Filenames to list
						rawFiles = chooser.getSelectedFiles();

						//Iterate through and add to active list
						for (int i = 0; i < rawFiles.length; i++) 
						{
							if (rawFiles[i].getName().endsWith(".mgf") || rawFiles[i].getName().endsWith(".mzXML"))
							{
								filesListModel.add(i, rawFiles[i].getAbsolutePath());
								mgfFiles.add(new File(rawFiles[i].getAbsolutePath()));
							}
							else throw new CustomException("Invalid file type.  Valid options are .mgf and .mzXML", null);
						}

						//Update button
						if (rawFiles.length>0)
						{
							searchSpectraButton.setEnabled(true);
						}
					}	
				}
				catch(CustomException er)
				{
					CustomError e1 = new CustomError(er.getMessage(), null);
				}
				catch(Exception er)
				{
					CustomError e1 = new CustomError(er.getMessage(), er);
				}
			}
		});

		btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				filesListModel.clear();
				searchSpectraButton.setEnabled(false);
				mgfFiles = new ArrayList<File>();
			}
		});

		JSeparator separator_1 = new JSeparator();
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(10)
					.addComponent(filesScrollPane, GroupLayout.DEFAULT_SIZE, 553, Short.MAX_VALUE)
					.addGap(11))
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
									.addContainerGap()
									.addComponent(selectAllButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(deselectAllButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE))
								.addGroup(groupLayout.createSequentialGroup()
									.addGap(10)
									.addComponent(availableLibsScroll, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE))
								.addGroup(groupLayout.createSequentialGroup()
									.addContainerGap()
									.addComponent(separator_1, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)))
							.addGap(23))
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblNewLabel)
							.addPreferredGap(ComponentPlacement.RELATED)))
					.addGap(1)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(searchSpectraButton, GroupLayout.PREFERRED_SIZE, 259, GroupLayout.PREFERRED_SIZE)
								.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 259, GroupLayout.PREFERRED_SIZE)
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
									.addComponent(separator, GroupLayout.PREFERRED_SIZE, 259, GroupLayout.PREFERRED_SIZE)
									.addComponent(lblNewLabel_2))
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(lblMsSearchTolerance_1, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
									.addGap(52)
									.addComponent(ms2TolBox, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE))
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
									.addGroup(groupLayout.createSequentialGroup()
										.addComponent(lowMassCutoff, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
										.addGap(52)
										.addComponent(lowMassBox, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE))
									.addGroup(groupLayout.createSequentialGroup()
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
											.addComponent(lblSearchResultsReturned, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE)
											.addComponent(lblMsSearchTolerance, GroupLayout.PREFERRED_SIZE, 182, GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
											.addComponent(maxSearchResultsSpinner, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE)
											.addComponent(ms1TolBox, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE)))))
							.addGap(21))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnAdd, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
							.addGap(6)
							.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE)
							.addGap(10))))
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(inputFilesLabel, GroupLayout.PREFERRED_SIZE, 158, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(406, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(inputFilesLabel)
					.addGap(2)
					.addComponent(filesScrollPane, GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
					.addGap(6)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel)
							.addComponent(lblNewLabel_2))
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(btnAdd)
								.addComponent(btnClear))
							.addGap(19)))
					.addGap(2)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(separator_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(10)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(ms1TolBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addGroup(groupLayout.createSequentialGroup()
									.addGap(3)
									.addComponent(lblMsSearchTolerance, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
								.addGroup(groupLayout.createSequentialGroup()
									.addGap(3)
									.addComponent(lblMsSearchTolerance_1))
								.addComponent(ms2TolBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
								.addComponent(maxSearchResultsSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addGroup(groupLayout.createSequentialGroup()
									.addGap(3)
									.addComponent(lblSearchResultsReturned, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
									.addGap(12)
									.addComponent(lowMassCutoff))
								.addGroup(groupLayout.createSequentialGroup()
									.addGap(9)
									.addComponent(lowMassBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
							.addGap(14)
							.addComponent(searchSpectraButton)
							.addGap(6))
						.addComponent(availableLibsScroll, GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(selectAllButton)
							.addComponent(deselectAllButton))
						.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE))
					.addGap(8))
		);
		getContentPane().setLayout(groupLayout);

	}

	//Method to select all boxes in library pane
	public static void selectAll(DefaultTableModel tableModel)
	{
		for (int i=0; i<tableModel.getRowCount(); i++)
		{
			tableModel.setValueAt(true, i, 1);
		}

	}

	//Method to deselect all boxes in library pane
	public static void deselectAll(DefaultTableModel tableModel)
	{
		for (int i=0; i<tableModel.getRowCount(); i++)
		{
			tableModel.setValueAt(false, i, 1);
		}

	}

	
	//Run Spectrum search
	private void startSearching(ArrayList<String> mzXMLFiles, ArrayList<String> mgfFiles, 
			JProgressBar progressBar, JTextField ms1TolBox, JTextField ms2TolBox, JSpinner maxSearchResultsSpinner)
	{
		worker = new SwingWorker<Void, Void>() 
				{
			@SuppressWarnings("unused")
			@Override
			protected Void doInBackground() throws Exception
			{
				//Run Searching
				try
				{
					setButtonStatus(false);
					//Initialize spectrum searcher
					SpectrumSearcher ss = new SpectrumSearcher(selectedLipidLibraries, mgfFiles, mzXMLFiles,progressBar, 
							Double.valueOf(ms1TolBox.getText()), Double.valueOf(ms2TolBox.getText()),
							(int)maxSearchResultsSpinner.getModel().getValue(),Double.valueOf(lowMassBox.getText()));

					//Run searcher
					ss.runSpectraSearch(selectedLipidLibraries,  Double.valueOf(ms2TolBox.getText()));
				}
				catch (CustomException e)
				{
					CustomError ce = new CustomError(e.getMessage(), null);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}

				return null;
			}
			@Override
			protected void done()
			{
				setButtonStatus(true);
			}
				};		
				worker.execute();
	}

	//Inactivate buttons while spectrum searcher is running
	private void setButtonStatus(Boolean status)
	{
		maxSearchResultsSpinner.setEnabled(status);
		ms2TolBox.setEnabled(status);
		ms1TolBox.setEnabled(status);
		filesList.setEnabled(status);
		lowMassBox.setEnabled(status);
		searchSpectraButton.setEnabled(status);
		selectAllButton.setEnabled(status);
		deselectAllButton.setEnabled(status);
		btnAdd.setEnabled(status); 
		btnClear.setEnabled(status); 
	}

	//Refresh library menu
	public void refreshLibraryMenu(JTable availableLibsTable, JScrollPane availableLibsScroll)
	{
		//Load all active libraries
		loadLibraries("src/msp_files");
		
		//Refresh table
		availableLibsTable.setModel(new DefaultTableModel(
				lipidLibNames,
				new String[] {
						"Library", "Enabled"
				})
		{boolean[] columnEditables = new boolean[] {false, true};
		public boolean isCellEditable(int row, int column) 
		{return columnEditables[column];}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 1 ? Boolean.class : super.getColumnClass(columnIndex);
		}});		

		availableLibsTable.getColumnModel().getColumn(1).setResizable(false);
		availableLibsTable.getColumnModel().getColumn(1).setMinWidth(75);
		availableLibsTable.getColumnModel().getColumn(1).setMaxWidth(75);
		availableLibsScroll.setViewportView(availableLibsTable);
		
		deselectAll((DefaultTableModel)availableLibsTable.getModel());
	}

	//Method to read in all available libraries (.msp) in libraries folder
	public void loadLibraries(String folder)
	{
		lipidLibraries = new ArrayList<File>();

		File[] files = new File(folder).listFiles();
		int i=0;

		if (new File(folder).exists())
		{
			for (File file : files) 
			{
				if (file.isFile() && file.getName().endsWith(".msp")) 
				{
					lipidLibraries.add(file);
				}
			}
		}

		lipidLibNames = new String[lipidLibraries.size()][2];

		for (File file : lipidLibraries) 
		{
			lipidLibNames[i][0] = file.getName().substring(0, file.getName().lastIndexOf("."));
			i++;
		}
	}
}
