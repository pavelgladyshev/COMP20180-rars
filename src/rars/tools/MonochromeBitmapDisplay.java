package rars.tools;

import rars.riscv.hardware.AccessNotice;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.MemoryAccessNotice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;

/*
Copyright (c) 2010-2011,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)
modified by Pavel Gladyshev (pavel.gladyshev@ucd.ie)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Monochrome bitmap display simulator.  It can be run either as a stand-alone Java application having
 * access to the rars package, or through RARS as an item in its Tools menu.  It makes
 * maximum use of methods inherited from its abstract superclass AbstractToolAndApplication.
 * Pete Sanderson, verison 1.0, 23 December 2010.
 */
public class MonochromeBitmapDisplay extends AbstractToolAndApplication {

    private static String version = "Version 1.0";
    private static String heading = "Monochrome Bitmap Display";

    // Major GUI components
    private Graphics drawingArea;
    private JPanel canvas;
    private JPanel results;

    // Some GUI settings
    private EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
    private Font countFonts = new Font("Times", Font.BOLD, 12);
    private Color backgroundColor = Color.WHITE;

    // Values for display canvas.  Note their initialization uses the identifiers just above.

    private int unitPixelWidth = 8;
    private int unitPixelHeight = 8;
    private int displayAreaWidthInPixels = 256;
    private int displayAreaHeightInPixels = 256;


    // The next four are initialized dynamically in initializeDisplayBaseChoices()
    private int baseAddress;

    private Grid theGrid;

    /**
     * Simple constructor, likely used to run a stand-alone bitmap display tool.
     *
     * @param title   String containing title for title bar
     * @param heading String containing text for heading shown in upper part of window.
     */
    public MonochromeBitmapDisplay(String title, String heading) {
        super(title, heading);
    }

    /**
     * Simple constructor, likely used by the RARS Tools menu mechanism
     */
    public MonochromeBitmapDisplay() {
        super("Monochrome Bitmap Display, " + version, heading);
    }


    /**
     * Main provided for pure stand-alone use.  Recommended stand-alone use is to write a
     * driver program that instantiates a Bitmap object then invokes its go() method.
     * "stand-alone" means it is not invoked from the RARS Tools menu.  "Pure" means there
     * is no driver program to invoke the application.
     */
    public static void main(String[] args) {
        new MonochromeBitmapDisplay("Monochrome Bitmap Display stand-alone, " + version, heading).go();
    }

    @Override
    public String getName() {
        return "Monochrome Bitmap Display";
    }

    /**
     * Override the inherited method, which registers us as an Observer over the static data segment
     * (starting address 0xffff4000) only.  This version will register us as observer over the
     * 32-word memory range (number of rows assuming that each 32-bit word represents).
     * It does so by calling the inherited 2-parameter overload of this method.
     * If you use the inherited GUI buttons, this
     * method is invoked when you click "Connect" button on Tool or the
     * "Assemble and Run" button on a Rars-based app.
     */
    protected void addAsObserver() {
        int highAddress = baseAddress + theGrid.getRows() * Memory.WORD_LENGTH_BYTES;
        // Special case: baseAddress<0 means we're in kernel memory (0x80000000 and up) and most likely
        // in memory map address space (0xffff0000 and up).  In this case, we need to make sure the high address
        // does not drop off the high end of 32 bit address space.  Highest allowable word address is 0xfffffffc,
        // which is interpreted in Java int as -4.
        if (baseAddress < 0 && highAddress > -4) {
            highAddress = -4;
        }
        addAsObserver(baseAddress, highAddress);
    }


    /**
     * Method that constructs the main display area.  It is
     * the visualization display which is updated as the
     * attached program executes.
     *
     * @return the GUI component
     */
    protected JComponent buildMainDisplayArea() {
        results = new JPanel();
        results.add(buildVisualizationArea());
        return results;
    }


    //////////////////////////////////////////////////////////////////////////////////////
    //  Rest of the protected methods.  These override do-nothing methods inherited from
    //  the abstract superclass.
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Update display when the connected program accesses (data) memory.
     *
     * @param memory       the attached memory
     * @param accessNotice information provided by memory in MemoryAccessNotice object
     */
    protected void processRISCVUpdate(Observable memory, AccessNotice accessNotice) {
        if (accessNotice.getAccessType() == AccessNotice.WRITE) {
            updateColorForAddress((MemoryAccessNotice) accessNotice);
        }
    }


    /**
     * Initialize all JComboBox choice structures not already initialized at declaration.
     * Overrides inherited method that does nothing.
     */
    protected void initializePreGUI() {
        theGrid = new Grid(displayAreaHeightInPixels / unitPixelHeight,
                displayAreaWidthInPixels / unitPixelWidth);
    }


    /**
     * The only post-GUI initialization is to create the initial Grid object based on the default settings
     * of the various combo boxes. Overrides inherited method that does nothing.
     */

    protected void initializePostGUI() {
        theGrid = createNewGrid();
        updateBaseAddress();
    }


    /**
     * Method to reset counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    protected void reset() {
        resetCounts();
        updateDisplay();
    }

    /**
     * Updates display immediately after each update (AccessNotice) is processed, after
     * display configuration changes as needed, and after each execution step when Rars
     * is running in timed mode.  Overrides inherited method that does nothing.
     */
    protected void updateDisplay() {
        canvas.repaint();
    }


    /**
     * Overrides default method, to provide a Help button for this tool/app.
     */
    protected JComponent getHelpComponent() {
        final String helpContent =
                "Use this program to simulate a simple monochrome bitmap display where\n" +
                        "each memory word starting from the address 0xffff8000\n" +
                        "represents a row of 32 pixels. This tool can be run from \n" + 
                        "Tools menu or as a stand-alone application.\n" +
                        "\n" +
                        "You can easily learn to use this small program by playing with\n" +
                        "it!   Each rectangular unit on the display corresponds to a single bit\n" +
                        "in a word word in a contiguous address space starting with the 0xffff8000\n" +
                        "base address.  The value stored in that word reepresents an entire row of pixels: \n" +
                        "The least significant bit represents the rightmost pixel, and the most significant \n" +
                        "bit represents the leftmost pixel. Each time a memory word within the display \n" +
                        "address space is written by the program, the corresponding row of pixels will be updated\n"+
                        "on the screen.\n" +
                        "\n" +
                        "Version 1.0 is very basic and was constructed from the Memory\n" +
                        "Reference Visualization tool's code.  Feel free to improve it and\n" +
                        "send your code for consideration in the next release.\n" +
                        "\n";
        JButton help = new JButton("Help");
        help.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JOptionPane.showMessageDialog(theWindow, helpContent);
                    }
                });
        return help;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //  Private methods defined to support the above.
    //////////////////////////////////////////////////////////////////////////////////////


    // UI components and layout for right half of GUI, the visualization display area.
    private JComponent buildVisualizationArea() {
        canvas = new GraphicsPanel();
        canvas.setPreferredSize(getDisplayAreaDimension());
        canvas.setToolTipText("Bitmap display area");
        return canvas;
    }

    // update based on combo box selection (currently not editable but that may change).
    private void updateBaseAddress() {
        baseAddress = 0xffff8000;
    }

    // Returns Dimension object with current width and height of display area as determined
    // by current settings of respective combo boxes.
    private Dimension getDisplayAreaDimension() {
        return new Dimension(displayAreaWidthInPixels, displayAreaHeightInPixels);
    }

    // reset all counters in the Grid.
    private void resetCounts() {
        theGrid.reset();
    }


      // Method to determine grid dimensions based on current control settings.
    // Each grid element corresponds to one visualization unit.
    private Grid createNewGrid() {
        int rows = displayAreaHeightInPixels / unitPixelHeight;
        int columns = displayAreaWidthInPixels / unitPixelWidth;
        return new Grid(rows, columns);
    }

    // Given memory address, update color for the corresponding grid element.
    private void updateColorForAddress(MemoryAccessNotice notice) {
        int address = notice.getAddress();
        int row = (address - baseAddress) / Memory.WORD_LENGTH_BYTES;
        int col,word;

        try {
            word = Memory.getInstance().getWord(address / Memory.WORD_LENGTH_BYTES * Memory.WORD_LENGTH_BYTES);
            for (col = 0; col<32; col++) 
            {
               theGrid.setElement(row, col, (word & (0x1 << (31-col))) != 0 ? 0x00ff00 : 0x000000);
            } 
                    
        } catch (Exception e) {
            // If address is out of range for display, do nothing.
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////
    //  Specialized inner classes for modeling and animation.
    //////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////////
    //  Class that represents the panel for visualizing and animating memory reference
    //  patterns.
    private class GraphicsPanel extends JPanel {

        // override default paint method to assure display updated correctly every time
        // the panel is repainted.
        public void paint(Graphics g) {
            paintGrid(g, theGrid);
        }

        // Paint the color codes.
        private void paintGrid(Graphics g, Grid grid) {
            int upperLeftX = 0, upperLeftY = 0;
            for (int i = 0; i < grid.getRows(); i++) {
                for (int j = 0; j < grid.getColumns(); j++) {
                    g.setColor(grid.getElementFast(i, j));
                    g.fillRect(upperLeftX, upperLeftY, unitPixelWidth, unitPixelHeight);
                    upperLeftX += unitPixelWidth;   // faster than multiplying
                }
                // get ready for next row...
                upperLeftX = 0;
                upperLeftY += unitPixelHeight;     // faster than multiplying
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////
    // Represents grid of colors
    private class Grid {

        Color[][] grid;
        int rows, columns;

        private Grid(int rows, int columns) {
            grid = new Color[rows][columns];
            this.rows = rows;
            this.columns = columns;
            reset();
        }

        private int getRows() {
            return rows;
        }

        private int getColumns() {
            return columns;
        }

        // Returns value in given grid element; null if row or column is out of range.
        private Color getElement(int row, int column) {
            return (row >= 0 && row <= rows && column >= 0 && column <= columns) ? grid[row][column] : null;
        }

        // Returns value in given grid element without doing any row/column index checking.
        // Is faster than getElement but will throw array index out of bounds exception if
        // parameter values are outside the bounds of the grid.
        private Color getElementFast(int row, int column) {
            return grid[row][column];
        }

        // Set the grid element.
        private void setElement(int row, int column, int color) {
            grid[row][column] = new Color(color);
        }

        // Set the grid element.
        private void setElement(int row, int column, Color color) {
            grid[row][column] = color;
        }

        // Just set all grid elements to black.
        private void reset() {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    grid[i][j] = Color.BLACK;
                }
            }
        }
    }

}