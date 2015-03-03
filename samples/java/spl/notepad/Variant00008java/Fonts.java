import java.awt.*; 
import java.awt.event.*; 
import javax.swing.*; 

/**
 *A class for creating JFontDialog
 */
public  class  Fonts  extends JDialog {
	
	private static final long serialVersionUID = 1L;

	
    /**
     *@see Center.java
     *this class to make the JDialog in the center
     */
    public Center center = new Center(this);

	

    //declaration of the private variables used in the program
    private JPanel jp = new JPanel();

	
    private JLabel fjl = new JLabel("Fonts: ");

	
    private JComboBox fjcb = new JComboBox();

	
    /**
     *-> GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames() <-
     *      WE USING THIS METHOD TO GET ALL FONT IN THE SYSTEM (OS)
     */
    private String fonts[]=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

	
    private JLabel sjl = new JLabel("Sizes: ");

	
    private JComboBox sjcb = new JComboBox();

	
    private String sizes[] = {"8","10","12","14","16","18","20","24","28","32","48","72"};

	
    private JLabel tjl = new JLabel("Types: ");

	
    private JComboBox tjcb = new JComboBox();

	
    private String types[] = {"Regular", "Bold", "Italic", "Bold Italic"};

	
    private JLabel jjl = new JLabel("Preview:");

	
    private JLabel jl = new JLabel("AaBaCcDdeEfFgGhHjJ");

	
    private JButton okjb = new JButton("OK");

	
    private JButton cajb = new JButton("Cancel");

	
    //for using ok & cancel button @Actions.java
    public JButton getOkjb(){
        return okjb;
    }

	
    public JButton getCajb(){
        return cajb;
    }

	
    //Constructor of Fonts
    public Fonts(){
        //for setting the title
        setTitle("Font Dialog");
        setResizable(false);
        /**
         *setting the layout (GridLayout: 5 rows & 2 columns)
         *add font JLabel, add font JComboBox
         *add type JLabel, add type JComboBox
         *add size JLabel, add size JComboBox
         *add preview JLabel,add test JLabel
         *add ok button, add cancel button
         */
        jp.setLayout(new GridLayout(5,2,1,1));
        jp.add(fjl);
        jp.add(fjcb = new JComboBox(fonts));
        jp.add(sjl);
        jp.add(sjcb = new JComboBox(sizes));
        jp.add(tjl);
        jp.add(tjcb = new JComboBox(types));
        jp.add(jjl);
        jl.setBorder(BorderFactory.createEtchedBorder());
        jp.add(jl);
        jp.add(okjb);
        jp.add(cajb);
        //add JPanel to the Container
        this.getContentPane().add(jp);
        /**
         *for making JDialog at the center,
         *@Center.java
         */
        center.fCenter();
        //add action listener to Font JComboBox to get the selected item for setting the preview
        fjcb.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
                jl.setFont(new Font(String.valueOf(fjcb.getSelectedItem()),tjcb.getSelectedIndex(),14));
            }
        });
        //add action listener to Type JComboBox to get the selected index for setting the preview
        tjcb.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
                jl.setFont(new Font(String.valueOf(fjcb.getSelectedItem()),tjcb.getSelectedIndex(),14));
            }
        });
    }

	
    /*
     *@return font value: (Font,Type,Size)
     */
    public Font font(){
        Font font = new Font(String.valueOf(fjcb.getSelectedItem()), tjcb.getSelectedIndex(),
                        Integer.parseInt(String.valueOf(sjcb.getSelectedItem())));
        return font;
    }


}
