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
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ImageIcon;

@SuppressWarnings("serial")
public class FileBrowser extends JInternalFrame {
	private JTable browserTable;	//JTable for all files added
	ArrayList<String> resultFiles;	//Arraylist of result files added to browser
	ArrayList<Integer> fileID;		//ArrayList of file identifiers for split polarity runs
	JTable parentTable;				//Parent table from PeakFinder window
	File lastDirectory;				//Last directory accessed

	//Launch the application
	public static void main(JTable guiTable, boolean[] separatePolaritiesArray, File lastDirectory) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FileBrowser frame = new FileBrowser(guiTable, separatePolaritiesArray, lastDirectory);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//Create frame
	public FileBrowser(JTable guiTable, boolean[] separatePolaritiesArray, File lastDirectory) {
		
		//Set GUI parameters
		setFrameIcon(new ImageIcon(FileBrowser.class.getResource("/icons/pf_16_icon.png")));
		parentTable = guiTable;
		setTitle("Results Uploader");
		setBounds(100, 100, 659, 528);
		setMinimumSize(new Dimension(601, 356));
		setClosable(true);
		this.setIconifiable(true);
		this.setResizable(true);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}
		
		//Initialize labels
		JLabel lblStepSelect = new JLabel("Step 1: Select data acquisition type");
		JLabel lblStepUpload = new JLabel("Step 2: Upload .csv result files from Spectrum Searcher");
		JSeparator separator = new JSeparator();
		JSeparator separator_1 = new JSeparator();
		JScrollPane browserPane = new JScrollPane();

		//Initialize browser table
		browserTable = new JTable();
		browserTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		browserTable.setForeground(Color.BLACK);
		DefaultTableModel tableModel = new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
						"File Name", "File ID"
				}
				) {
			@SuppressWarnings("rawtypes")
			Class[] columnTypes = new Class[] {
					Object.class, Integer.class
			};

			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
			
			@SuppressWarnings("unused")
			public boolean[] columnEditables = new boolean[] {
					false, true
			};
			private boolean editable = true;

			@SuppressWarnings("unused")
			public boolean isEditable() {
				return editable;
			}

			public boolean isCellEditable(int row, int column) {
				if (column==0) return false;
				else if (column==1) return true;
				return true;
			}
		};

		browserTable.setModel(tableModel);
		browserTable.setAutoCreateRowSorter(true);
		browserTable.getColumnModel().getColumn(1).setResizable(false);
		browserTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		browserTable.getColumnModel().getColumn(1).setMinWidth(50);
		browserTable.getColumnModel().getColumn(1).setMaxWidth(50);
		TableColumn fileIDColumn = browserTable.getColumnModel().getColumn(1);
		browserTable.removeColumn(browserTable.getColumnModel().getColumn(1));


		JRadioButton polaritySwitchingRadio = new JRadioButton("Polarity Switching");
		polaritySwitchingRadio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browserTable.getColumnModel().removeColumn(browserTable.getColumnModel().getColumn(1));
			}
		});
		polaritySwitchingRadio.setSelected(true);

		JRadioButton separatePolarityRadio = new JRadioButton("Separate Polarity Analyses");
		separatePolarityRadio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browserTable.getColumnModel().addColumn(fileIDColumn);
			}
		});
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(polaritySwitchingRadio);
		buttonGroup.add(separatePolarityRadio);

		JButton browseButton = new JButton("Add Files");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				//Create Array List
				File[] rawFiles;

				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						".csv files", "csv");
				chooser.setMultiSelectionEnabled(true);
				chooser.setFileFilter(filter);
				chooser.setCurrentDirectory(lastDirectory);

				int returnVal = chooser.showOpenDialog(null);

				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{

					//Add Filenames to list
					rawFiles = chooser.getSelectedFiles();

					//Iterate through and add to active list
					for (int i = 0; i < rawFiles.length; i++) 
					{
						if (polaritySwitchingRadio.isSelected()) tableModel.addRow(new Object[]{rawFiles[i].getPath(),(i+1)});
						else tableModel.addRow(new Object[]{rawFiles[i].getPath(),0});
					}
				}	
			}
		});

	
		JLabel lblStepFor = new JLabel("Step 3: For separate polarity analyses, input file ID number");

		JSeparator separator_2 = new JSeparator();

		JButton uploadButton = new JButton("Upload");
		uploadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parentTable.setModel(browserTable.getModel());
				if (polaritySwitchingRadio.isSelected())
				{
					((DefaultTableModel)parentTable.getModel()).setColumnCount(1);
					separatePolaritiesArray[0] = false;
				}
				else
				{
					separatePolaritiesArray[0] = true;
				}
				dispose();
			}
		});

		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tableModel.setRowCount(0);
			}
		});
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(browserPane, GroupLayout.DEFAULT_SIZE, 623, Short.MAX_VALUE)
								.addGroup(groupLayout.createSequentialGroup()
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												.addComponent(separator, GroupLayout.PREFERRED_SIZE, 272, GroupLayout.PREFERRED_SIZE)
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(polaritySwitchingRadio)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(separatePolarityRadio))
														.addComponent(lblStepSelect)
														.addComponent(separator_1, GroupLayout.PREFERRED_SIZE, 279, GroupLayout.PREFERRED_SIZE)
														.addComponent(lblStepUpload)
														.addComponent(browseButton))
														.addGap(279))
														.addComponent(separator_2, GroupLayout.PREFERRED_SIZE, 284, GroupLayout.PREFERRED_SIZE)
														.addComponent(lblStepFor)
														.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
																.addComponent(clearButton)
																.addPreferredGap(ComponentPlacement.RELATED)
																.addComponent(uploadButton)))
																.addContainerGap())
				);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addGap(19)
						.addComponent(lblStepSelect)
						.addGap(2)
						.addComponent(separator, GroupLayout.PREFERRED_SIZE, 1, GroupLayout.PREFERRED_SIZE)
						.addGap(6)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(polaritySwitchingRadio)
								.addComponent(separatePolarityRadio))
								.addGap(18)
								.addComponent(lblStepUpload)
								.addGap(2)
								.addComponent(separator_1, GroupLayout.PREFERRED_SIZE, 1, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(browseButton)
								.addGap(18)
								.addComponent(lblStepFor)
								.addGap(2)
								.addComponent(separator_2, GroupLayout.PREFERRED_SIZE, 1, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(browserPane, GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
										.addComponent(uploadButton)
										.addComponent(clearButton))
										.addGap(5))
				);
		browserPane.setViewportView(browserTable);
		getContentPane().setLayout(groupLayout);

	}

}
