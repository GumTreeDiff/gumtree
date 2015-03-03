import javax.swing.*; 

/**
 *A CLASS FOR CREATING ABOUT PANEL
 */
public  class  About  extends JPanel {
	
	private static final long serialVersionUID = 1;

	
    public About(){
        //Create a Label & an image icon in it
        JLabel label1 = new JLabel(new ImageIcon(this.getClass().getResource("images/java.gif")));
        //adding label1 to the JPanel
        this.add(label1);
        //Create a Label & put a HTML script
        JLabel label2 = new JLabel("<html><li>JAVA??? Notepad</li><li><p>Ver# 2.0</li>"
        +"<li><p>Coded by: Salah Al-Thubaiti</li><li><p>KFUPM, CS</li><li>"
        +"<p>CopyRight??? 2001-2002</li></html>");
        //adding label2 to the JPanel
        this.add(label2);
    }


}
