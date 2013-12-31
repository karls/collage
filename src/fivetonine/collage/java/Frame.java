package fivetonine.collage;

import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;

public class Frame {
    public static JFrame createImageFrame(String title, BufferedImage image) {
	JFrame frame = new JFrame(title);
	ImageIcon icon = new ImageIcon(image);
	JLabel imageLabel = new JLabel(icon);

	frame.add(imageLabel);
	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	frame.pack();
	frame.show();
	return frame;
    }
}
