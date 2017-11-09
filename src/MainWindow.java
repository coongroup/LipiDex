import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import javax.swing.Icon;
import javax.swing.JToolBar;
import lib_gen.CustomError;
import lib_gen.LibrarySelector;
import lib_gen.LipidGenGUI;
import lib_gen.SpectrumGenerator;
import javax.swing.JDesktopPane;
import peak_finder.PeakFinderGUI;
import spectrum_searcher.SpectrumSearcherGUI;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {

	private JDesktopPane contentPane;					//Main DesktopPane to add internal frames to
	private JPanel final_pane;							//
	private LipidGenGUI lg;						//Library generator instance
	SpectrumSearcherGUI ss;								//Spectrum searcher instance
	PeakFinderGUI pf;									//Peak Finder instance
	private JToolBar toolbar_menu;						//Top toolbar for menu items
	private JPanel gray_background;						//Grey background for initial menu
	private JLabel ld_logo;								//LipiDex logo
	private JLabel coon_logo;							//Coon lab logo
	private SpectrumGenerator sg;						//Spectrum generator instance
	private LibrarySelector ls = null;					//Library selector for LG and SG initialization
	private String[] selectedLibrary = new String[1];	//Currently active library
	
	/**
	 * Create the frame.
	 * @throws IOException 
	 */
	public MainWindow() {
		
		//Set look and feel to default for users computer
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native LAF: " + e);
		}
		
		//Initialize window final pane
		final_pane = new JPanel();
		final_pane.setBorder(null);
		setContentPane(final_pane);
		final_pane.setLayout(new BorderLayout(0, 0));

		//Set window dimension
		setMinimumSize(new Dimension(700, 590));
		setTitle("LipiDex v0.9.1");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1285, 828);
		
		//Initialize desktop pane
		contentPane = new JDesktopPane();
		contentPane.setBackground(new Color(13, 37, 45));
		contentPane.setBorder(null);
		final_pane.add(contentPane, BorderLayout.CENTER);
		
		//Add menu labels
		JLabel ss_menu_Label = new JLabel(new ImageIcon("src/Icons/ss_menu.png"));
		JLabel pf_menu_Label = new JLabel(new ImageIcon("src/Icons/pf_menu.png"));
		JLabel lg_menu_Label = new JLabel(new ImageIcon("src/Icons/lg_menu.png"));
		JLabel sg_menu_Label = new JLabel(new ImageIcon("src/Icons/sg_menu.png"));
		
		//Initialize spectrum searcher
		try {
			ss = new SpectrumSearcherGUI(ss_menu_Label,
					new ImageIcon("src/Icons/ss_menu_open.png"),
					new ImageIcon("src/Icons/ss_menu.png"));
		} catch (IOException e2) {
			@SuppressWarnings("unused")
			CustomError e = new CustomError("Could not access GUI files", e2);
		}
		ss_menu_Label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try {
					ss.refreshLibraryMenu(ss.availableLibsTable, ss.availableLibsScroll);
					ss.setIcon(false);
					ss.setVisible(true);
					ss.toFront();
				} catch (PropertyVetoException e) 
				{
					@SuppressWarnings("unused")
					CustomError ce = new CustomError("", e);
				}
			}
		});
		ss.setVisible(false);
		ss.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
		ss.setResizable(true);
		contentPane.add(ss);

		//Initialize Peak Finder
		pf = new PeakFinderGUI(contentPane, pf_menu_Label,
				new ImageIcon("src/Icons/pf_menu_open.png"),
				new ImageIcon("src/Icons/pf_menu.png"));
		pf.setNormalBounds(new Rectangle(1, 1, 579, 685));
		pf.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
		pf.setVisible(false);
		contentPane.add(pf);

		//Initialize LipiDex and Coon Lab logo
		ld_logo = new JLabel(new ImageIcon("src/Icons/LipiDex_Logo.png"));
		coon_logo = new JLabel(new ImageIcon("src/Icons/Coon_Lab_Logo.png"));
		coon_logo.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				URI uri = null;
				try {
					uri = new URI("http://coonlabs.com/");
				} catch (URISyntaxException e) {
				}
				Desktop desktop = Desktop.getDesktop();
				try {
					desktop.browse(uri);
				} catch (IOException e) {
				}
			}
		});
		coon_logo.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				coon_logo.setIcon(new ImageIcon("src/Icons/Coon_Lab_Logo_Hover.png"));
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent evt) {
				coon_logo.setIcon(new ImageIcon("src/Icons/Coon_Lab_Logo.png"));
			}
		});

		//Initialize main menu
		gray_background = new JPanel();
		gray_background.setBackground(new Color(129, 141, 153));

		toolbar_menu = new JToolBar();
		toolbar_menu.setBorder(null);
		toolbar_menu.setBackground(new Color(129, 141, 153));
		toolbar_menu.setFloatable(false);
		final_pane.add(toolbar_menu, BorderLayout.NORTH);

		//Menu Item Buttons
		lg_menu_Label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try {
					//If library searcher window opened and no active library generator
					if(ls !=null && (ls.lg == null || !ls.lg.isVisible()))
					{
							ls = new LibrarySelector(getLibraries("src/Libraries"), selectedLibrary, lg, 
									contentPane, lg_menu_Label, new ImageIcon("src/Icons/lg_menu_open.png"), 
									new ImageIcon("src/Icons/lg_menu.png"));
							ls.setVisible(true);
							ls.setIcon(false);
							ls.setSelected(true);
							contentPane.add(ls);
							ls.moveToFront();
					}
					//If no library searcher every opened
					else if ((ls == null))
					{
						ls = new LibrarySelector(getLibraries("src/Libraries"), selectedLibrary, lg, 
								contentPane, lg_menu_Label, new ImageIcon("src/Icons/lg_menu_open.png"), 
								new ImageIcon("src/Icons/lg_menu.png"));
						ls.setVisible(true);
						ls.setIcon(false);
						ls.setSelected(true);
						contentPane.add(ls);
						ls.moveToFront();
					}
					//Else if active library generator
					else if (ls.lg != null || ls.lg.isVisible())
					{	
						ls.lg.setIcon(false);
						ls.lg.toFront();
					}
				} catch (PropertyVetoException e) {
					@SuppressWarnings("unused")
					CustomError ce = new CustomError("", e);
					e.printStackTrace();
				}
			}
		});
		toolbar_menu.add(lg_menu_Label);
		JLabel separatorLabel2 = new JLabel(new ImageIcon("src/Icons/menu_separator.png"));
		toolbar_menu.add(separatorLabel2);

		sg_menu_Label.addMouseListener(new MouseAdapter()
		{
			@SuppressWarnings("unused")
			public void mouseClicked(MouseEvent evt)
			{
				try 
				{
					ls = new LibrarySelector(getLibraries("src/Libraries"), selectedLibrary, sg, 
							contentPane, sg_menu_Label, new ImageIcon("src/Icons/sg_menu_open.png"), 
							new ImageIcon("src/Icons/sg_menu.png"));
					ls.setVisible(true);
					ls.setIcon(false);
					ls.setSelected(true);
					contentPane.add(ls);
					ls.toFront();
				} catch (Exception e) {
					CustomError e1 = new CustomError(e.getLocalizedMessage(), null);
				}

			}
		});
		toolbar_menu.add(sg_menu_Label);
		JLabel separatorLabel4 = new JLabel(new ImageIcon("src/Icons/menu_separator.png"));
		toolbar_menu.add(separatorLabel4);
		toolbar_menu.add(ss_menu_Label);
		JLabel separatorLabel5 = new JLabel(new ImageIcon("src/Icons/menu_separator.png"));
		toolbar_menu.add(separatorLabel5);
		pf_menu_Label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try {
					closeMainMenu();
					pf.setIcon(false);
					pf.setVisible(true);
					pf.toFront();
					pf_menu_Label.setIcon(new ImageIcon("src/Icons/pf_menu_open.png"));
				} catch (PropertyVetoException e) {
					@SuppressWarnings("unused")
					CustomError e1 = new CustomError(e.getLocalizedMessage(), null);
				}
			}
		});
		toolbar_menu.add(pf_menu_Label);
		JLabel separatorLabel6 = new JLabel(new ImageIcon("src/Icons/menu_separator.png"));
		toolbar_menu.add(separatorLabel6);
		toolbar_menu.setVisible(false);

		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
				gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addGap(35)
						.addComponent(ld_logo, GroupLayout.PREFERRED_SIZE, 301, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED, 721, Short.MAX_VALUE)
						.addComponent(coon_logo, GroupLayout.PREFERRED_SIZE, 242, GroupLayout.PREFERRED_SIZE)
						.addGap(35))
						.addComponent(gray_background, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 1284, Short.MAX_VALUE)
				);
		gl_contentPane.setVerticalGroup(
				gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addGap(167)
						.addComponent(gray_background, GroupLayout.PREFERRED_SIZE, 177, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED, 211, Short.MAX_VALUE)
						.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(ld_logo, GroupLayout.PREFERRED_SIZE, 172, GroupLayout.PREFERRED_SIZE)
								.addComponent(coon_logo, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE))
								.addGap(35))
				);

		
		//Initialize top menu labels and functionality
		JLabel library_generator_label = new JLabel(new ImageIcon("src/Icons/library_generator_icon.png"));
		library_generator_label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				//library_generator_label.setIcon(new ImageIcon("src/Icons/library_generator_icon_gray.png"));
				clickAnimation(library_generator_label, "src/Icons/library_generator_icon_gray.png");
				closeMainMenu();

				try {
					//If library searcher window opened and no active library generator
					if(ls !=null && (ls.lg == null || !ls.lg.isVisible()))
					{
							ls = new LibrarySelector(getLibraries("src/Libraries"), selectedLibrary, lg, 
									contentPane, lg_menu_Label, new ImageIcon("src/Icons/lg_menu_open.png"), 
									new ImageIcon("src/Icons/lg_menu.png"));
							ls.setVisible(true);
							ls.setIcon(false);
							ls.setSelected(true);
							ls.toFront();
							contentPane.add(ls);
					}
					//If no library searcher every opened
					else if ((ls == null))
					{
						ls = new LibrarySelector(getLibraries("src/Libraries"), selectedLibrary, lg, 
								contentPane, lg_menu_Label, new ImageIcon("src/Icons/lg_menu_open.png"), 
								new ImageIcon("src/Icons/lg_menu.png"));
						ls.setVisible(true);
						ls.setIcon(false);
						ls.setSelected(true);
						ls.toFront();
						contentPane.add(ls);
					}
					//Else if active library generator
					else if (ls.lg != null || ls.lg.isVisible())
					{	
						ls.lg.setIcon(false);
						ls.lg.toFront();
					}
				} catch (PropertyVetoException e) {
					@SuppressWarnings("unused")
					CustomError e1 = new CustomError(e.getLocalizedMessage(), null);
				}
			}
		});
		library_generator_label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				library_generator_label.setIcon(new ImageIcon("src/Icons/library_generator_icon_gray.png"));
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent evt) {
				library_generator_label.setIcon(new ImageIcon("src/Icons/library_generator_icon.png"));
			}
		});


		JLabel spectrum_generator_label = new JLabel(new ImageIcon("src/Icons/spectrum_generator_icon.png"));
		spectrum_generator_label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try {
					closeMainMenu();
					ls = new LibrarySelector(getLibraries("src/Libraries"), selectedLibrary, sg, 
							contentPane, sg_menu_Label, new ImageIcon("src/Icons/sg_menu_open.png"), 
							new ImageIcon("src/Icons/sg_menu.png"));
					ls.setIcon(false);
					ls.setVisible(true);
					ls.setSelected(true);
					ls.toFront();
					contentPane.add(ls);
				} catch (Exception e) {
					@SuppressWarnings("unused")
					CustomError e1 = new CustomError(e.getLocalizedMessage(), null);
				}

			}
		}
				);
		spectrum_generator_label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				spectrum_generator_label.setIcon(new ImageIcon("src/Icons/spectrum_generator_icon_gray.png"));
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent evt) {
				spectrum_generator_label.setIcon(new ImageIcon("src/Icons/spectrum_generator_icon.png"));
			}
		});

		JLabel spectrum_searcher_label = new JLabel(new ImageIcon("src/Icons/spectrum_searcher_icon.png"));
		spectrum_searcher_label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try {
					ss.refreshLibraryMenu(ss.availableLibsTable, ss.availableLibsScroll);
					closeMainMenu();
					ss.setIcon(false);
					ss.setVisible(true);
					ss.toFront();
					ss_menu_Label.setIcon(new ImageIcon("src/Icons/ss_menu_open.png"));
				} catch (PropertyVetoException e) {
					@SuppressWarnings("unused")
					CustomError e1 = new CustomError(e.getLocalizedMessage(), null);
				}
			}
		});
		spectrum_searcher_label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				spectrum_searcher_label.setIcon(new ImageIcon("src/Icons/spectrum_searcher_icon_gray.png"));
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent evt) {
				spectrum_searcher_label.setIcon(new ImageIcon("src/Icons/spectrum_searcher_icon.png"));
			}
		});


		JLabel peak_finder_label = new JLabel(new ImageIcon("src/Icons/peak_finder_icon.png"));
		peak_finder_label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				try {
					closeMainMenu();
					pf.setIcon(false);
					pf.setVisible(true);
					pf.toFront();
					pf_menu_Label.setIcon(new ImageIcon("src/Icons/pf_menu_open.png"));
				} catch (PropertyVetoException e) {
					@SuppressWarnings("unused")
					CustomError e1 = new CustomError(e.getLocalizedMessage(), null);
				}
			}
		});
		peak_finder_label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				peak_finder_label.setIcon(new ImageIcon("src/Icons/peak_finder_icon_gray.png"));
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent evt) {
				peak_finder_label.setIcon(new ImageIcon("src/Icons/peak_finder_icon.png"));
			}
		});

		JLabel front_spacer = new JLabel((Icon) null);
		JLabel back_spacer = new JLabel((Icon) null);

		
		//Initialize group layout
		GroupLayout gl_gray_background = new GroupLayout(gray_background);
		gl_gray_background.setHorizontalGroup(
				gl_gray_background.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_gray_background.createSequentialGroup()
						.addComponent(front_spacer, GroupLayout.DEFAULT_SIZE, 177, 10000)
						.addComponent(library_generator_label, GroupLayout.DEFAULT_SIZE, 177, 192)
						.addGap(20)
						.addComponent(spectrum_generator_label, GroupLayout.DEFAULT_SIZE, 177, 192)
						.addGap(20)
						.addComponent(spectrum_searcher_label, GroupLayout.DEFAULT_SIZE, 177, 192)
						.addGap(20)
						.addComponent(peak_finder_label, GroupLayout.DEFAULT_SIZE, 177, 192)
						.addComponent(back_spacer, GroupLayout.DEFAULT_SIZE, 182, 10000))
				);
		gl_gray_background.setVerticalGroup(
				gl_gray_background.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_gray_background.createSequentialGroup()
						.addGap(6)
						.addGroup(gl_gray_background.createParallelGroup(Alignment.LEADING)
								.addComponent(front_spacer, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE)
								.addComponent(library_generator_label, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE)
								.addComponent(spectrum_generator_label, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE)
								.addComponent(spectrum_searcher_label, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE)
								.addComponent(peak_finder_label, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE)
								.addComponent(back_spacer, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE))
								.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				);
		gray_background.setLayout(gl_gray_background);
		contentPane.setLayout(gl_contentPane);

		//Set window icon
		try {
			List<Image> icons = new ArrayList<Image>();
			icons.add(ImageIO.read(new File("src/Icons/LipiDex_Icon_Blue_48.png")));
			icons.add(ImageIO.read(new File("src/Icons/LipiDex_Icon_Blue_16.png")));
			setIconImages(icons);
			//setIconImage();
		}


		catch (IOException exc) {
			exc.printStackTrace();
		}

		setVisible(true);
	}

	//Get all libraries in library folder
	private ArrayList<String> getLibraries(String folder)
	{
		File file = new File(folder);
		ArrayList<String> result = new ArrayList<String>(Arrays.asList(file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		})));
		return result;
	}

	//Animation for button clicling
	private void clickAnimation(JLabel label, String icon1)
	{
		label.setIcon(new ImageIcon(icon1));
		Rectangle progressRect = label.getBounds();
		progressRect.x = 0;
		progressRect.y = 0;
		label.paintImmediately(progressRect);
	}

	//Method to close main menu once option selected
	private void closeMainMenu()
	{
		gray_background.setVisible(false);
		contentPane.remove(gray_background);
		toolbar_menu.setVisible(true);
		try {
			TimeUnit.MILLISECONDS.sleep(140);
		} catch (InterruptedException e) {
		}
	}
}
