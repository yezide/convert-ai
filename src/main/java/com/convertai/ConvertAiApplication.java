package com.convertai;

import com.convertai.config.AppConfig;
import com.convertai.service.AliBailianClient;
import com.convertai.service.DirectoryConversionService;
import com.convertai.service.FfmpegService;
import com.convertai.service.ImageTitleOverlayService;
import com.convertai.ui.MainFrame;

import javax.swing.SwingUtilities;

public class ConvertAiApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppConfig config = AppConfig.load();
            AliBailianClient aiClient = new AliBailianClient(config);
            FfmpegService ffmpegService = new FfmpegService(config);
            ImageTitleOverlayService imageTitleOverlayService = new ImageTitleOverlayService(config);
            DirectoryConversionService conversionService = new DirectoryConversionService(config, aiClient, ffmpegService, imageTitleOverlayService);
            new MainFrame(conversionService).setVisible(true);
        });
    }
}
