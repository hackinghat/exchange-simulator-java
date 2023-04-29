package com.hackinghat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class App extends JPanel implements ActionListener {
    private final static Logger LOG = LogManager.getLogger(App.class);
    private final static int NITEMS = 100;
    private final static int YRANGE = 100;

    private final int[] entries = new int[NITEMS];
    private final Timer timer;
    private final Random r;
    private int insertAt = 0;
    private int xPos = 0;

    public App() {
        r = new Random(0);
        timer = new Timer(100, this);
        timer.start();

    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.getContentPane().add(new App());
        f.setSize(300, 200);
        f.setVisible(true);
        f.setResizable(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = new Font("Serif", Font.PLAIN, 96);
        g2.setFont(font);
        int w = pixelsPerX();
        int h = pixelsPerY();
        LOG.debug("paint");
        for (int i = 0; i < xPos - 1; ++i) {
            LOG.debug("x1 = " + w * i + ", y1 = " + h * entries[i] + ", x2 = " + w * (i + 1) + ", y2 = " + h * entries[i + 1]);
            g2.drawLine(w * i, h * entries[i], w * (i + 1), h * entries[i + 1]);
        }
    }

    int pixelsPerX() {
        return getWidth() / NITEMS;
    }

    int pixelsPerY() {
        return getHeight() / YRANGE;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == timer) {
            entries[insertAt++] = r.nextInt(YRANGE);
            if (insertAt == entries.length)
                insertAt = 0;
            xPos++;
            repaint();
        }
    }
}
