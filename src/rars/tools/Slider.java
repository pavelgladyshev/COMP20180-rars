package rars.tools;

import rars.Globals;
import rars.riscv.hardware.AddressErrorException;
import rars.riscv.hardware.Memory;
import rars.util.Binary;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class Slider extends AbstractToolAndApplication{

    // Menu/Program info (Title, header, memory location, etc.)
    private static String title = "Slider tool";
    private static String header = "Slider tool - Sebastian Drayne"; // I don't know how to write these v_v
    private final int SLIDER_MEM_LOCATION;


    // GUI
    private static JPanel SliderSlide;
    private static SliderComp slider;
    private static CheckComp checkbox;

    // Slider values/settings
    private boolean doubleMode = false;


    public static void main(String[] args) {
        new Slider(title, header);
    }
    public Slider() {
        this(title, header);
    }

    public Slider(String title, String heading) {
        super(title, heading);
        SLIDER_MEM_LOCATION = Memory.memoryMapBaseAddress + 0x90;
    }

    @Override
    public String getName() {
        return "Slider";
    }

    public void addAsObserver() {
        addAsObserver(SLIDER_MEM_LOCATION, SLIDER_MEM_LOCATION);
    }

    // I copy-pasted this from DigitalLabSim.java.
    protected JComponent getHelpComponent() {
        final String helpContent = "A simple implementation of a slider component. Outputs to " + Binary.intToHexString(SLIDER_MEM_LOCATION) +
                """
                .\nThe check box outputs the slider's value as a half. This enables more precision.
                (Contributed by Didier Teifreto, dteifreto@lifc.univ-fcomte.fr, modified by Pavel Gladyshev and Sebastian Drayne)
                """;
        JButton help = new JButton("Help");
        help.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JTextArea ja = new JTextArea(helpContent);
                        ja.setRows(20);
                        ja.setColumns(60);
                        ja.setLineWrap(true);
                        ja.setWrapStyleWord(true);
                        JOptionPane.showMessageDialog(theWindow, new JScrollPane(ja),
                                "Simulating a slider component", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
        return help;
    }

    @Override
    protected JComponent buildMainDisplayArea() {
        SliderSlide = new JPanel(new GridLayout(2, 1));

        // Component(s)
        slider = new SliderComp();
        SliderSlide.add(slider);

        checkbox = new CheckComp();
        SliderSlide.add(checkbox);

        return SliderSlide;
    }

    public void updateValue(int dataAddress, int dataValue) {
        // Most of this was stolen from DigitalLabSim.java. Sorry!
        if(!this.isBeingUsedAsATool || (this.isBeingUsedAsATool && connectButton.isConnected())) {
            Globals.memoryAndRegistersLock.lock();
            try {
                try {
                    if(!doubleMode) {
                        Globals.memory.setByte(dataAddress, dataValue);
                    }
                    else {
                        Globals.memory.setHalf(dataAddress, dataValue);
                    }
                } catch (AddressErrorException aee) {
                    System.out.println("Tool author specified incorrect slider value address!\n" + aee);
                    System.exit(0);
                }
            } finally {
                Globals.memoryAndRegistersLock.unlock();
            }
            if (Globals.getGui() != null && Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().getCodeHighlighting()) {
                Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().updateValues();
            }
        }
    }

    public void reset() {
        slider.resetSlider();
        checkbox.resetCheckbox();
    }



    public class SliderComp extends JPanel {
        private JSlider slider;

        public SliderComp() {
            // Set to 65,024 as if you need more precision you shouldn't be working with a 2nd year's Java code.
            this.slider = new JSlider(0, 65535);
            this.slider.addChangeListener(new slideUpdate());
            this.add(slider);
            this.setPreferredSize(new Dimension(100, 20));
        }

        // Sets the slider to half
        public void resetSlider() {
            slider.setValue(32767);
        }

        public class slideUpdate implements ChangeListener {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(doubleMode) {
                    // If it's double mode it uses the full thing. Otherwise, it uses the value / 255
                    updateValue(SLIDER_MEM_LOCATION, slider.getValue());
                }
                else {
                    // Int version of it.
                    updateValue(SLIDER_MEM_LOCATION, slider.getValue() / 256);
                }
            }
        }
    }

    // This increases precision.
    public class CheckComp extends JPanel {
        private JCheckBox cb;
        public CheckComp() {
            this.cb = new JCheckBox("Enable half");
            this.add(cb);

            this.cb.addItemListener(e -> {
                doubleMode = (e.getStateChange() == ItemEvent.SELECTED);
                // Causes issues otherwise
                slider.resetSlider();
            });
        }

        public void resetCheckbox() {
            cb.setSelected(false);
        }

    }
}
