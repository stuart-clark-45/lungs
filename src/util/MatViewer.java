package util;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import org.opencv.core.Mat;

/**
 * Debugging utility to enable the viewing of a list of {@link Mat}s in order. Use the left and
 * right arrow keys to change image.
 * 
 * @author Stuart Clark
 */
public class MatViewer {

  private CircularList<BufferedImage> images;
  private List<String> matTitles;
  private Object lock;
  public int currentImage;


  public MatViewer(List<Mat> mats) {
    this(mats, defaultTitles(mats.size()));
  }

  public MatViewer(List<Mat> mats, List<String> matTitles) {
    this.images =
        new CircularList<>(mats.parallelStream().map(MatUtils::toBufferedImage)
            .collect(Collectors.toList()));
    this.matTitles = new CircularList<>(matTitles);
    this.lock = new Object();
  }

  /**
   * Display the images. N.B. This method is blocking.
   */
  public void display() {
    // Create a panel for the image
    final JPanel panel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics g2 = g.create();
        g2.drawImage(images.get(currentImage), 0, 0, getWidth(), getHeight(), null);
        g2.dispose();
      }

      @Override
      public Dimension getPreferredSize() {
        BufferedImage img = images.get(currentImage);
        return new Dimension(img.getWidth(), img.getHeight());
      }
    };

    // Add the panel to a JFrame
    final JFrame frame = new JFrame();
    frame.setTitle(matTitles.get(currentImage));
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.add(panel);
    frame.pack();
    frame.setVisible(true);

    // Create a new thread that will stop when lock.notify() is called
    Thread t = new Thread(() -> {
      synchronized (lock) {
        while (frame.isVisible())
          try {
            lock.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
      }
    });
    t.start();

    // Listen for the JFrame closed event and call lock.notify()
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent arg0) {
        synchronized (lock) {
          frame.setVisible(false);
          lock.notify();
        }
      }
    });

    // Listen for key presses that are used to change the image being displayed
    frame.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
          currentImage += 1;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
          currentImage -= 1;
        }

        frame.setTitle(matTitles.get(currentImage));
        frame.repaint();
      }
    });

    // Wait for the JFrame to be closed.
    try {
      t.join();
    } catch (InterruptedException e) {
      // Don't want exception to be thrown by this method as will need to be handled each time this
      // class is used for debugging.
      throw new RuntimeException(e);
    }

  }

  /**
   * @param numMats
   * @return a list of default titles for the {@code numMats} {@link Mat}s
   */
  private static List<String> defaultTitles(int numMats) {
    List<String> names = new ArrayList<>(numMats);
    for (int i = 1; i <= numMats; i++) {
      names.add(i + "/" + numMats);
    }
    return names;
  }

}
