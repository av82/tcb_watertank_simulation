
/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
/**
 *
 * @author arun
 *
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

class RnCTL {

    public boolean runOK;
}

class SimData1 implements Runnable {

    public static final int left = 60;
    public static final int top = 100;
    public static final int width = 150;
    public static final int height = 200;
    public static final int llheight = 40;
    public static final int loheight = 80;
    public static final int hiheight = 120;
    public static final int hhheight = 160;
    public static final int inset = 10; // thickness of water tank

    public JButton pmp1;
    public JButton pmp2;
    public JButton pmp3;
    public JButton pmp4;
    public JButton pmp5;
    public JButton vlv1;
    public JButton vlv2;
    public JButton vlv3;
    public JButton vlv4;
    public JButton vlv5;

    public static final Color tankColor = new Color(127, 127, 127);
    private static final Color fillColor = new Color(25, 25, 112);
    private static final Color emptyColor = new Color(240, 248, 255);
    private static final int xstr = 500;
    private static final int ystr = 40;
    private static final int x1str = 300;
    private static final int y1str = 40;
    public JPanel displayPanel;
    public JPanel displayPanel1;
    public JPanel displayPanel2;
    public JPanel displayPanel3;
    public JPanel displayPanel4;
    public bPanel MainDispaly;
    public int sysid;
    public int timeval;
    public static boolean systate;
    public int[] Level = new int[10];
    public boolean[] pump = new boolean[10];
    public boolean[] valve = new boolean[10];
    public boolean[] hh = new boolean[10];
    public boolean[] hi = new boolean[10];
    public boolean[] lo = new boolean[10];
    public boolean[] ll = new boolean[10];

    public boolean[] pumpOR = new boolean[10];
    public boolean[] valveOR = new boolean[10]; // overwrite vals

    public boolean[] Pstate = new boolean[5];
    public boolean[] Vstate = new boolean[5];

    public static String state;
    public static String title;
    public boolean KILL;
    public int increment;
    private int sleepTime;
    private RnCTL check;
    private String DataString;
    public static final String InFIFO = "GETFIFO";
    public static final String OutFIFO = "SETFIFO";

    private static final String FIFO = "temp";
    String line = null;
    BufferedReader inBuff = null;
    String[] tokens;

    public SimData1(RnCTL rc) {
        check = rc;
        timeval = 0;
        KILL = false;
        restart();
        java.util.Arrays.fill(pump, false);

        java.util.Arrays.fill(valve, false);

        java.util.Arrays.fill(hh, false);

        java.util.Arrays.fill(hi, false);

        java.util.Arrays.fill(lo, false);

        java.util.Arrays.fill(ll, false);
        java.util.Arrays.fill(pumpOR, false);
        java.util.Arrays.fill(valveOR, false);
        java.util.Arrays.fill(Pstate, false);
        java.util.Arrays.fill(Vstate, false);

    }

    public void restart() {
        // currentDepth = 0;
        state = "Restarting";
        increment = 2;
        sleepTime = 500;

    }

    // to set random heights at initialization only
    public void rndvals() {
        int START = 1;
        int END = 160;
        Random random = new Random();
        for (int i = 0; i < 5; ++i) {
            Level[i] = RandomRange.getRandomValue(START, END, random); // all levels

            if (Level[i] < llheight) {
                valve[i] = false;
                pump[i] = true;

            } else if (Level[i] > hiheight) {
                pump[i] = false;
            } else {
                valve[i] = true;
            }

            Pstate[i] = pump[i];
            Vstate[i] = valve[i];
        }

    }

    private void simulate() {

        double demand;
        demand = Math.random();
        DataString = "|" + timeval++ + "|";

        // if level < LL turn pump on, turn valve off
        // if level > LL and valve if off then valve ON
        // if level >HH pump is off

        int pp, vv, hh1, hi1, lo1, ll1, killsim;

        for (int i = 0; i < 5; i++) {

            // if manual overwrite is NOT set then set values
            if (pumpOR[i] || valveOR[i]) {
                GUI.lmanual.setText("Manual On" + Integer.toString(i));
            }

            if (Level[i] < llheight) {
                if (!valveOR[i])
                    valve[i] = false;
                if (!pumpOR[i])
                    pump[i] = true;

            }
            if ((Level[i] > llheight)) {
                if (!valveOR[i])
                    valve[i] = true;

            }
            if (Level[i] >= hhheight) {
                if (!pumpOR[i])
                    pump[i] = false;
            }

            if (pump[i]) {

                Level[i] = Level[i] + increment;
            }
            if (valve[i]) {

                Level[i] = Level[i] - (int) (demand * increment);
            }

            if (Level[i] >= llheight) {
                ll[i] = true;
            } else {
                ll[i] = false;
            }

            if (Level[i] >= loheight) {
                lo[i] = true;
            } else {
                lo[i] = false;
            }

            if (Level[i] >= hiheight) {
                hi[i] = true;
            } else {
                hi[i] = false;
            }

            if (Level[i] >= hhheight) {
                hh[i] = true;
            } else {
                hh[i] = false;
            }

            pp = pump[i] ? 1 : 0;
            vv = valve[i] ? 1 : 0;
            hh1 = hh[i] ? 1 : 0;
            hi1 = hi[i] ? 1 : 0;
            lo1 = lo[i] ? 1 : 0;
            ll1 = ll[i] ? 1 : 0;

            DataString = DataString + pp + "|" + vv + "|" + hh1 + "|" + hi1 + "|" + lo1 + "|" + ll1 + "|";
            System.out.println(DataString);
        }

        killsim = KILL ? 1 : 0;
        DataString = DataString + killsim + "|";

    }

    public void setLinkToPanel(bPanel mPanel, JPanel aPanel, JPanel bPanel, JPanel cPanel, JPanel dPanel,
            JPanel ePanel) {

        displayPanel = aPanel;
        displayPanel1 = bPanel;
        displayPanel2 = cPanel;
        displayPanel3 = dPanel;
        displayPanel4 = ePanel;

        MainDispaly = mPanel;

        displayPanel.setBackground(Color.BLACK);
        displayPanel1.setBackground(Color.BLACK);
        displayPanel2.setBackground(Color.BLACK);
        displayPanel3.setBackground(Color.BLACK);
        displayPanel4.setBackground(Color.BLACK);

    }

    public void paint(Graphics g) {

        g.setColor(SimData1.tankColor);
        g.fillRoundRect(displayPanel.getX() + SimData1.left, SimData1.top, SimData1.width, SimData1.height, 20, 20);
        g.setColor(Color.BLACK);
        g.fillRect(displayPanel.getX() + SimData1.inset + SimData1.left, SimData1.top,
                SimData1.width - 2 * SimData1.inset, SimData1.height - SimData1.inset);

        g.setColor(fillColor);
        g.fillRect(displayPanel.getX() + SimData1.inset + SimData1.left, top + height - inset - Level[0],
                SimData1.width - 2 * SimData1.inset, Level[0]);

        if (!hh[0]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 160,
                15, 8, true);
        if (!hi[0]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 120,
                15, 8, true);

        if (!lo[0]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 80,
                15, 8, true);

        if (!ll[0]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 40,
                15, 8, true);

        g.setColor(SimData1.tankColor);
        g.fillRoundRect(displayPanel1.getX() + SimData1.left, SimData1.top, SimData1.width, SimData1.height, 20, 20);
        g.setColor(Color.BLACK);
        g.fillRect(displayPanel1.getX() + SimData1.inset + SimData1.left, SimData1.top,
                SimData1.width - 2 * SimData1.inset, SimData1.height - SimData1.inset);

        g.setColor(fillColor);
        g.fillRect(displayPanel1.getX() + SimData1.inset + SimData1.left, top + height - inset - Level[1],
                SimData1.width - 2 * SimData1.inset, Level[1]);

        if (!hh[1]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel1.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 160,
                15, 8, true);

        if (!hi[1]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel1.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 120,
                15, 8, true);

        if (!lo[0]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel1.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 80,
                15, 8, true);

        if (!ll[0]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel1.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 40,
                15, 8, true);

        g.setColor(SimData1.tankColor);
        g.fillRoundRect(displayPanel2.getX() + SimData1.left, SimData1.top, SimData1.width, SimData1.height, 20, 20);
        g.setColor(Color.BLACK);
        g.fillRect(displayPanel2.getX() + SimData1.inset + SimData1.left, SimData1.top,
                SimData1.width - 2 * SimData1.inset, SimData1.height - SimData1.inset);

        g.setColor(fillColor);
        g.fillRect(displayPanel2.getX() + SimData1.inset + SimData1.left, top + height - inset - Level[2],
                SimData1.width - 2 * SimData1.inset, Level[2]);

        if (!hh[2]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel2.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 160,
                15, 8, true);

        if (!hi[2]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel2.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 120,
                15, 8, true);

        if (!lo[2]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel2.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 80,
                15, 8, true);

        if (!ll[2]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel2.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 40,
                15, 8, true);

        g.setColor(SimData1.tankColor);
        g.fillRoundRect(displayPanel3.getX() + SimData1.left, SimData1.top, SimData1.width, SimData1.height, 20, 20);
        g.setColor(Color.BLACK);
        g.fillRect(displayPanel3.getX() + SimData1.inset + SimData1.left, SimData1.top,
                SimData1.width - 2 * SimData1.inset, SimData1.height - SimData1.inset);

        g.setColor(fillColor);
        g.fillRect(displayPanel3.getX() + SimData1.inset + SimData1.left, top + height - inset - Level[3],
                SimData1.width - 2 * SimData1.inset, Level[3]);

        if (!hh[3]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel3.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 160,
                15, 8, true);
        if (!hi[3]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel3.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 120,
                15, 8, true);
        if (!lo[3]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel3.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 80,
                15, 8, true);
        if (!ll[3]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel3.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 40,
                15, 8, true);

        g.setColor(SimData1.tankColor);
        g.fillRoundRect(displayPanel4.getX() + SimData1.left, SimData1.top, SimData1.width, SimData1.height, 20, 20);
        g.setColor(Color.BLACK);
        g.fillRect(displayPanel4.getX() + SimData1.inset + SimData1.left, SimData1.top,
                SimData1.width - 2 * SimData1.inset, SimData1.height - SimData1.inset);
        g.setColor(fillColor);
        g.fillRect(displayPanel4.getX() + SimData1.inset + SimData1.left, top + height - inset - Level[4],
                SimData1.width - 2 * SimData1.inset, Level[4]);

        if (!hh[4]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel4.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 160,
                15, 8, true);

        if (!hi[4]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }

        g.fill3DRect(displayPanel4.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 120,
                15, 8, true);
        if (!lo[4]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel4.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 80,
                15, 8, true);

        if (!ll[4]) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.RED);
        }
        g.fill3DRect(displayPanel4.getX() + SimData1.left - 3, SimData1.top + (SimData1.height - SimData1.inset) - 40,
                15, 8, true);

    }

    public void ReadWriteFIFO() {

    }

    public void run() {

        for (;;) {
            synchronized (check) {
                while (!check.runOK) {
                    try {
                        check.wait();
                    } catch (InterruptedException ie) {
                    }
                }

            }
            try {
                simulate();

                FileWriter fstream = new FileWriter(OutFIFO);
                BufferedWriter out = new BufferedWriter(fstream);
                // out.write("|1|20|1|0|1|1|1|1|");
                out.write(DataString);

                for (int i = 0; i < 5; i++) {
                    Pstate[i] = pump[i];
                    Vstate[i] = valve[i];
                }

                if (pump[0]) {
                    GUI.plight[0].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));

                } else {
                    GUI.plight[0].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));

                }
                if (pump[1]) {
                    GUI.plight[1].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.plight[1].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }
                if (pump[2]) {
                    GUI.plight[2].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.plight[2].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }
                if (pump[3]) {
                    GUI.plight[3].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.plight[3].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }

                if (pump[4]) {
                    GUI.plight[4].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.plight[4].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }
                if (valve[0]) {
                    GUI.vlight[0].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.vlight[0].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }
                if (valve[1]) {
                    GUI.vlight[1].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.vlight[1].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }
                if (valve[2]) {
                    GUI.vlight[2].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.vlight[2].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }
                if (valve[3]) {
                    GUI.vlight[3].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.vlight[3].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }
                if (valve[4]) {
                    GUI.vlight[4].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                } else {
                    GUI.vlight[4].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                }

                GUI.lbltime.setText(Integer.toString(timeval));
                GUI.lbltime.setForeground(Color.GREEN);

                System.out.println(DataString); // Close the output stream
                out.close();

            } catch (Exception e) {// Catch exception if any
                System.err.println("Error: " + e.getMessage());
            }

            String line = null;
            inBuff = null;
            while (true) {
                try {
                    // System.out.println("JAVA SIDE!!");
                    inBuff = new BufferedReader(new FileReader(InFIFO));
                    while ((line = inBuff.readLine()) != null) {
                        String delims = "[ ]+";
                        String[] tokens = line.split(delims);

                        for (int i = 0; i < tokens.length; i++) {
                            System.out.print("System " + (i + 1) + "state: " + tokens[i]);

                            if (Integer.parseInt(tokens[i]) == 0) {
                                GUI.lblsystate.setText(Integer.toString(i + 1));
                                GUI.lblsystate.setForeground(Color.RED);
                                if (i != 5) {
                                    GUI.sbalarm[i]
                                            .setIcon(new javax.swing.ImageIcon(getClass().getResource("smallred.png")));
                                }

                                else

                                {
                                    GUI.alarmImage.setIcon(
                                            new javax.swing.ImageIcon(getClass().getResource("red_light.png")));
                                }
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException ie) {
                                }

                            } else {
                                if (i != 5) {
                                    GUI.sbalarm[i].setIcon(
                                            new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
                                }

                                else {
                                    GUI.alarmImage.setIcon(
                                            new javax.swing.ImageIcon(getClass().getResource("green_light.png")));

                                }

                            }

                            System.out.println();

                        }

                        break;

                    }
                    // done, however you can choose to cycle over this line
                    // in this thread or launch another to check for new input
                    inBuff.close();
                    break;

                } catch (IOException ex) {
                    System.err.println("IO Exception at buffered read!!");
                    System.exit(-1);
                }

            }
            MainDispaly.repaint();

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }
        }

    }
}

class RandomRange {

    public static int getRandomValue(int aStart, int aEnd, Random aRandom) {
        if (aStart > aEnd) {
            throw new IllegalArgumentException("Start cannot exceed End.");
        }
        long range = (long) aEnd - (long) aStart + 1;

        long fraction = (long) (range * aRandom.nextDouble());
        return (int) (fraction + aStart);

    }

    private static void log(String aMessage) {
        System.out.println(aMessage);
    }
}

class MyPanel extends JPanel {

    SimData1 fData;

    public MyPanel(SimData1 aData) {
        setPreferredSize(new Dimension(200, 200));
        fData = aData;
    }

    public void paint(Graphics g) {
        super.paint(g);

    }
}

class bPanel extends JPanel {

    SimData1 fData;

    public bPanel(SimData1 aData) {
        setPreferredSize(new Dimension(800, 400));
        fData = aData;
    }

    public void paint(Graphics g) {
        super.paint(g);
        fData.paint(g);

    }
}

class BtmPanel extends JPanel {

    public BtmPanel() {
        setPreferredSize(new Dimension(800, 100));

    }
}

class SubBtmPanel extends JPanel {

    public SubBtmPanel() {
        setPreferredSize(new Dimension(800, 200));

    }
}

class GUI extends JFrame implements ActionListener {

    private boolean running;
    public MyPanel fPanel1;
    public MyPanel fPanel2;
    public MyPanel fPanel3;
    public MyPanel fPanel4;
    public MyPanel fPanel5;
    public static bPanel fPanel;
    public BtmPanel btmPanel;
    public SubBtmPanel subbtmPanel;
    public JButton btnContinue;
    public JButton SimContinue;
    private SimData1 fData;
    private Thread fWorkThread;
    private RnCTL fCheck;
    public static JLabel lbltime;
    public static JLabel alarmImage;
    public static JLabel[] sbalarm = new JLabel[5];
    public static JLabel[] plight = new JLabel[5];
    public static JLabel[] vlight = new JLabel[5];

    public static JLabel lblsystate;
    public static JLabel lmanual;

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == fData.pmp1) {

            fData.pumpOR[0] = !fData.pumpOR[0];

            fData.Pstate[0] = !fData.Pstate[0];
            fData.pump[0] = fData.Pstate[0];
        }

        if (e.getSource() == fData.pmp2) {

            fData.pumpOR[1] = !fData.pumpOR[1];

            fData.Pstate[1] = !fData.Pstate[1];
            fData.pump[1] = fData.Pstate[1];

        }
        if (e.getSource() == fData.pmp3) {
            fData.pumpOR[2] = !fData.pumpOR[2];

            fData.Pstate[2] = !fData.Pstate[2];
            fData.pump[2] = fData.Pstate[2];

        }
        if (e.getSource() == fData.pmp4) {
            fData.pumpOR[3] = !fData.pumpOR[3];

            fData.Pstate[3] = !fData.Pstate[3];
            fData.pump[3] = fData.Pstate[3];

        }
        if (e.getSource() == fData.pmp5) {
            fData.pumpOR[4] = !fData.pumpOR[4];

            fData.Pstate[4] = !fData.Pstate[4];
            fData.pump[4] = fData.Pstate[4];

        }

        if (e.getSource() == fData.vlv1) {
            fData.valveOR[0] = !fData.valveOR[0];

            fData.Vstate[0] = !fData.Vstate[0];
            fData.valve[0] = fData.Vstate[0];

        }
        if (e.getSource() == fData.vlv2) {
            fData.valveOR[1] = !fData.valveOR[1];

            fData.Vstate[1] = !fData.Vstate[1];
            fData.valve[1] = fData.Vstate[1];

        }
        if (e.getSource() == fData.vlv3) {
            fData.valveOR[2] = !fData.valveOR[2];

            fData.Vstate[2] = !fData.Vstate[2];
            fData.valve[2] = fData.Vstate[2];

        }
        if (e.getSource() == fData.vlv4) {
            fData.valveOR[3] = !fData.valveOR[3];

            fData.Vstate[3] = !fData.Vstate[3];
            fData.valve[3] = fData.Vstate[3];

        }
        if (e.getSource() == fData.vlv5) {
            fData.valveOR[4] = !fData.valveOR[4];

            fData.Vstate[4] = !fData.Vstate[4];
            fData.valve[4] = fData.Vstate[4];

        }

    }

    public GUI() {

        fCheck = new RnCTL();

        fData = new SimData1(fCheck);

        GridLayout panellayout = new GridLayout(1, 5, 5, 0);

        this.setLayout(new BorderLayout(5, 0));

        fPanel1 = new MyPanel(fData);
        fPanel2 = new MyPanel(fData);
        fPanel3 = new MyPanel(fData);
        fPanel4 = new MyPanel(fData);
        fPanel5 = new MyPanel(fData);

        lbltime = new JLabel();

        fPanel = new bPanel(fData);
        fPanel.setLayout(panellayout);
        fPanel.setBackground(Color.BLACK);

        fData.pmp1 = new JButton("P1");
        fData.vlv1 = new JButton("V1");
        fData.pmp2 = new JButton("P2");
        fData.vlv2 = new JButton("V2");
        fData.pmp3 = new JButton("P3");
        fData.vlv3 = new JButton("V3");
        fData.pmp4 = new JButton("P4");
        fData.vlv4 = new JButton("V4");
        fData.pmp5 = new JButton("P5");
        fData.vlv5 = new JButton("V5");

        fData.pmp1.setPreferredSize(new Dimension(95, 28));
        fData.vlv1.setPreferredSize(new Dimension(95, 28));

        fData.pmp2.setPreferredSize(new Dimension(95, 28));
        fData.vlv2.setPreferredSize(new Dimension(95, 28));
        fData.pmp3.setPreferredSize(new Dimension(95, 28));
        fData.vlv3.setPreferredSize(new Dimension(95, 28));
        fData.pmp4.setPreferredSize(new Dimension(95, 28));
        fData.vlv4.setPreferredSize(new Dimension(95, 28));
        fData.pmp5.setPreferredSize(new Dimension(95, 28));
        fData.vlv5.setPreferredSize(new Dimension(95, 28));

        // fStartButton.setPreferredSize(new Dimension(30, 30));
        fData.pmp1.addActionListener(this);
        fData.pmp2.addActionListener(this);
        fData.pmp3.addActionListener(this);
        fData.pmp4.addActionListener(this);
        fData.pmp5.addActionListener(this);

        fData.vlv1.addActionListener(this);
        fData.vlv2.addActionListener(this);
        fData.vlv3.addActionListener(this);
        fData.vlv4.addActionListener(this);
        fData.vlv5.addActionListener(this);

        fPanel1.add(fData.pmp1);
        fPanel1.add(fData.vlv1);

        fPanel2.add(fData.pmp2);
        fPanel2.add(fData.vlv2);
        fPanel3.add(fData.pmp3);
        fPanel3.add(fData.vlv3);

        fPanel4.add(fData.pmp4);
        fPanel4.add(fData.vlv4);
        fPanel5.add(fData.pmp5);
        fPanel5.add(fData.vlv5);

        fPanel.add(fPanel1);
        fPanel.add(fPanel2);
        fPanel.add(fPanel3);
        fPanel.add(fPanel4);
        fPanel.add(fPanel5);

        fPanel.setBackground(Color.BLACK);
        fPanel.validate();
        fData.setLinkToPanel(fPanel, fPanel1, fPanel2, fPanel3, fPanel4, fPanel5);

        alarmImage = new JLabel();
        alarmImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("green_light.png")));

        sbalarm[0] = new JLabel();
        sbalarm[0].setPreferredSize(new Dimension(80, 70));
        sbalarm[0].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));

        plight[0] = new JLabel();
        plight[0].setPreferredSize(new Dimension(80, 50));
        plight[0].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        vlight[0] = new JLabel();
        vlight[0].setPreferredSize(new Dimension(80, 50));
        vlight[0].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        fPanel1.add(plight[0]);
        fPanel1.add(vlight[0]);

        sbalarm[1] = new JLabel();
        sbalarm[1].setPreferredSize(new Dimension(80, 70));
        sbalarm[1].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));

        plight[1] = new JLabel();
        plight[1].setPreferredSize(new Dimension(80, 50));
        plight[1].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        vlight[1] = new JLabel();
        vlight[1].setPreferredSize(new Dimension(80, 50));
        vlight[1].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        fPanel2.add(plight[1]);
        fPanel2.add(vlight[1]);

        plight[2] = new JLabel();
        plight[2].setPreferredSize(new Dimension(80, 50));
        plight[2].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        vlight[2] = new JLabel();
        vlight[2].setPreferredSize(new Dimension(80, 50));
        vlight[2].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        fPanel3.add(plight[2]);
        fPanel3.add(vlight[2]);

        plight[3] = new JLabel();
        plight[3].setPreferredSize(new Dimension(80, 50));
        plight[3].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        vlight[3] = new JLabel();
        vlight[3].setPreferredSize(new Dimension(80, 50));
        vlight[3].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        fPanel4.add(plight[3]);
        fPanel4.add(vlight[3]);

        plight[4] = new JLabel();
        plight[4].setPreferredSize(new Dimension(80, 50));
        plight[4].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        vlight[4] = new JLabel();
        vlight[4].setPreferredSize(new Dimension(80, 50));
        vlight[4].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));
        fPanel5.add(plight[4]);
        fPanel5.add(vlight[4]);

        sbalarm[2] = new JLabel();
        sbalarm[2].setPreferredSize(new Dimension(80, 70));
        sbalarm[2].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));

        sbalarm[3] = new JLabel();
        sbalarm[3].setPreferredSize(new Dimension(80, 70));
        sbalarm[3].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));

        sbalarm[4] = new JLabel();
        sbalarm[4].setPreferredSize(new Dimension(80, 70));
        sbalarm[4].setIcon(new javax.swing.ImageIcon(getClass().getResource("smallgreen.png")));

        btmPanel = new BtmPanel();
        subbtmPanel = new SubBtmPanel();

        btmPanel.setBackground(Color.BLACK);
        subbtmPanel.setBackground(Color.BLACK);

        btmPanel.add(alarmImage, BorderLayout.CENTER);

        subbtmPanel.add(sbalarm[0], BorderLayout.EAST);
        subbtmPanel.add(sbalarm[1], BorderLayout.EAST);
        subbtmPanel.add(sbalarm[2], BorderLayout.EAST);
        subbtmPanel.add(sbalarm[3], BorderLayout.EAST);
        subbtmPanel.add(sbalarm[4], BorderLayout.EAST);

        subbtmPanel.validate();

        btnContinue = new JButton();
        SimContinue = new JButton("Continue Simulation");

        btnContinue.setPreferredSize(new Dimension(150, 50));
        SimContinue.setPreferredSize(new Dimension(180, 50));

        btnContinue.addActionListener(this);
        SimContinue.addActionListener(this);
        SimContinue.setVisible(false);
        btnContinue.setVisible(false);

        lbltime.setPreferredSize(new Dimension(80, 70));

        lblsystate = new JLabel();
        lblsystate.setPreferredSize(new Dimension(80, 70));

        lmanual = new JLabel();
        lmanual.setPreferredSize(new Dimension(80, 70));
        lmanual.setText("SIM");

        lmanual.setForeground(Color.WHITE);

        btmPanel.add(lblsystate);

        btmPanel.add(lbltime);

        btmPanel.add(lmanual);

        btmPanel.validate();

        add(fPanel, BorderLayout.NORTH);
        add(subbtmPanel, BorderLayout.CENTER);
        add(btmPanel, BorderLayout.SOUTH);

        this.setBackground(Color.BLACK);

        this.pack();
        running = true;
        fData.rndvals();
        fCheck.runOK = true;

        fWorkThread = new Thread(fData);
        fWorkThread.start();

    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // fData.paint(g);

    }
}

public class JWTank {

    public static void main(String[] args) {

        try {
            // Set cross-platform Java L&F (also called "Metal")
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");

        } catch (UnsupportedLookAndFeelException e) {
            // handle exception
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }
        GUI aGUI = new GUI();
        aGUI.setSize(800, 800);
        aGUI.setVisible(true);
    }
}
