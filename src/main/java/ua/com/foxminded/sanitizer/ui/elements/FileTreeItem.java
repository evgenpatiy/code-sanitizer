package ua.com.foxminded.sanitizer.ui.elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.NoArgsConstructor;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.data.FileData;
import ua.com.foxminded.sanitizer.ui.FileViewWindow;
import ua.com.foxminded.sanitizer.worker.FileWorker;
import ua.com.foxminded.sanitizer.worker.OSWorker.OS;

@NoArgsConstructor
public class FileTreeItem extends TreeItem<File> implements ISanitizerEnvironment {
    public class CustomFileTreeCell extends TextFieldTreeCell<File> {
        @Override
        public void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty) {
                if (item.isFile()) {
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem mi1 = new MenuItem("View " + item.getName());
                    mi1.setOnAction(event -> new FileViewWindow(item.toString()).show());
                    contextMenu.getItems().add(mi1);
                    setContextMenu(contextMenu);
                }
                setEditable(false);
                this.setText(item.getName());
            }
        }
    }

    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;
    private String modifiedFileString;
    private String ownerFileString;
    private String permissionsFileString;
    private String sizeFileString;
    private String contentFileString;
    private Image folderCollapsedImage = new Image(getClass().getResourceAsStream("/img/folder.png"));
    private Image folderOpenedImage = new Image(getClass().getResourceAsStream("/img/folder_open.png"));
    private Image fileImage = new Image(getClass().getResourceAsStream("/img/file.png"));
    private ObservableList<FileData> dataView;
    private ArrayList<FileData> fileList = new ArrayList<FileData>();
    private TableView<FileData> tableView = new TableView<FileData>();
    private FileWorker fileWorker = new FileWorker();

    public FileTreeItem(File file) {
        super(file);
        setMessages();
        setGraphic(file.isDirectory() ? new ImageView(folderCollapsedImage) : new ImageView(fileImage));

        addEventHandler(TreeItem.branchExpandedEvent(), event -> {
            TreeItem<Object> source = event.getSource();
            if (source.isExpanded()) {
                ImageView iv = (ImageView) source.getGraphic();
                iv.setImage(folderOpenedImage);
                processDirectory(Paths.get(source.getValue().toString()));
            }
        });
        addEventHandler(TreeItem.branchCollapsedEvent(), event -> {
            TreeItem<Object> source = event.getSource();
            if (!source.isExpanded()) {
                ImageView iv = (ImageView) source.getGraphic();
                iv.setImage(folderCollapsedImage);
            }
        });
    }

    public void setMessages() {
        contentFileString = "Type: ";
        modifiedFileString = "Modified: ";
        ownerFileString = "Owner: ";
        permissionsFileString = "Permissions: ";
        sizeFileString = "Size: ";
    }

    public String getToolTipText(String fileName) {
        File file = new File(fileName);
        String result = file.getName() + System.lineSeparator() + "------" + System.lineSeparator();
        try {
            result += contentFileString + " " + fileWorker.getFileContentType(file) + System.lineSeparator();
            result += modifiedFileString + " " + fileWorker.getFileTime(file) + System.lineSeparator();
            if ((ENV == OS.MAC) || (ENV == OS.UNIX) || (ENV == OS.SOLARIS)) {
                Path currentFilePath = Paths.get(file.getAbsolutePath());
                result += ownerFileString + " " + Files.getOwner(currentFilePath, LinkOption.NOFOLLOW_LINKS)
                        + System.lineSeparator();
                result += permissionsFileString + " "
                        + fileWorker.getPermissions(
                                Files.getPosixFilePermissions(currentFilePath, LinkOption.NOFOLLOW_LINKS))
                        + System.lineSeparator();
            }
            result += "------" + System.lineSeparator();
            result += sizeFileString + " " + file.length();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public TableView<FileData> getTableView() {
        dataView = FXCollections.observableArrayList(fileList);

        TableColumn<FileData, String> indexCol = new TableColumn<FileData, String>("#");
        indexCol.setCellFactory(column -> new TableCell<FileData, String>() {
            @Override
            public void updateIndex(int index) {
                super.updateIndex(index);
                setText((isEmpty() || index < 0) ? null : Integer.toString(index + 1));
            }
        });

        TableColumn<FileData, String> filenameCol = new TableColumn<FileData, String>("File (context menu available)");
        filenameCol.setCellValueFactory(new PropertyValueFactory<FileData, String>("fileName"));

        tableView.setRowFactory(tv -> {
            final TableRow<FileData> row = new TableRow<>();
            final MenuItem mi1 = new MenuItem();
            mi1.setOnAction(event -> new FileViewWindow(row.getItem().getFileName()).show());

            row.setOnContextMenuRequested(event -> mi1
                    .setText(row.isEmpty() ? null : "View " + Paths.get(row.getItem().getFileName()).getFileName()));
            row.emptyProperty().addListener((observable, wasEmpty, isEmpty) -> {
                row.setContextMenu(isEmpty ? null : new ContextMenu(mi1));
                row.setTooltip(isEmpty ? null : new Tooltip(getToolTipText(row.getItem().getFileName())));
            });
            return row;
        });
        tableView.setItems(dataView);
        tableView.getColumns().addAll(indexCol, filenameCol);
        tableView.getSortOrder().add(indexCol);
        tableView.getSortOrder().add(filenameCol);
        indexCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.10));
        indexCol.setResizable(true);
        filenameCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.90));
        filenameCol.setResizable(true);
        tableView.setPlaceholder(new Label("no proper files here"));
        return tableView;
    }

    @Override
    public ObservableList<TreeItem<File>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            File file = (File) getValue();
            isLeaf = file.isFile();
        }
        return isLeaf;
    }

    private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
        File file = TreeItem.getValue();
        if (file != null && file.isDirectory()) {
            // дерево каталогов слева, FileFilter на каталог
            File[] files = file.listFiles(pathname -> {
                return (!pathname.isHidden()) && pathname.isDirectory();
            });

            if (files != null) {
                ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
                Arrays.stream(files).forEach(f -> children.add(new FileTreeItem(f)));
                return children;
            }
        }
        return FXCollections.emptyObservableList();
    }

    public void processDirectory(Path dir) {
        fileList.clear();
        File[] files = dir.toFile().listFiles(pathname -> {
            return !pathname.isHidden() && (!pathname.isDirectory());
        });

        for (File file : files) {
            FileData tableItem = new FileData();
            tableItem.setFileName(file.getAbsolutePath());
            fileList.add(tableItem);
        }
        dataView = FXCollections.observableArrayList(fileList);
        tableView.setItems(dataView);
    }
}
