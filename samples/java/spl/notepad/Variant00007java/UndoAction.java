import java.awt.event.*; 
import javax.swing.*; 
import javax.swing.undo.*; 

 

class  UndoAction  extends AbstractAction {
	
	private static final long serialVersionUID = 1;

	 
	Notepad notepad;

	

    public UndoAction(Notepad notepad){
        super( "Undo" );
        putValue( Action.SMALL_ICON,
                                new ImageIcon( this.getClass().getResource( "images/undo.gif" ) ) );
        setEnabled( false );
        this.notepad = notepad;
    }

	
    public void actionPerformed( ActionEvent e ) {
        try {
            notepad.undo.undo();
        }
        catch ( CannotUndoException ex ) {
            System.out.println( "Unable to undo: " + ex );
            ex.printStackTrace();
        }
        update();
        notepad.redoAction.update();
    }

	
    protected void update() {
        if( notepad.undo.canUndo() ) {
            setEnabled( true );
            putValue( "Undo", notepad.undo.getUndoPresentationName() );
        }
        else {
            setEnabled( false );
            putValue( Action.NAME, "Undo" );
        }
    }


}
