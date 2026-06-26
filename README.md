# convert-ai

PC 桌面批处理工具：选择剧集目录后，调用阿里百炼生成新简介、新封面，并用 FFmpeg 给视频改名、加水印、转 1080P，所有输出写入所选目录下的 `convert` 子目录。

## 运行

1. 编辑 `config/application.properties`，填写 `dashscope.api-key`。
2. 将 `ffmpeg.exe` 放到程序目录的 `bin/ffmpeg.exe`，或修改 `ffmpeg.path`。
3. 打包并运行：

```bash
mvn clean package
java -jar target/convert-ai-1.0.0.jar
```

## 支持的文件

- 文本：`.txt`, `.md`
- 图片：`.jpg`, `.jpeg`, `.png`, `.webp`, `.bmp`
- 视频：`.mp4`, `.mkv`, `.mov`, `.avi`, `.flv`, `.wmv`, `.m4v`

视频编号会从文件名中提取数字，例如 `猫和老鼠-01.mp4`、`[2]猫和老鼠.mp4`。
