package SC_Application;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.ResourceBundle;
import java.util.Scanner;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.*;

class Settings {
	public String lastSavePath;
	public boolean sort = false;
};

public class SC_Controller implements Initializable {

	@FXML
	private ComboBox<String> line_input;
	@FXML
	private ListView<String> add_files_list;
	@FXML
	private Label drag_text;
	@FXML
	private Button save_btn;
	@FXML
	private Button delete_btn;
	@FXML
	private Button save_script_btn;

	private List<String> raw_files = new ArrayList<String>();
	private ObservableList<String> processed_files = FXCollections.observableArrayList();
	private ObservableList<String> template_choices = FXCollections.observableArrayList();
	
	private ContextMenu context_menu = new ContextMenu();
	private MenuItem menu_remove = new MenuItem("Remove Item");
	private MenuItem menu_clear = new MenuItem("Clear All");

	private int selected_item;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		Settings settings = new Settings();
		load();
		
		drag_text.setTextFill(Color.LIGHTGRAY);
		context_menu.getItems().addAll(menu_remove, menu_clear);
		
		save_script_btn.setDisable(true);
		
		add_files_list.setOnDragOver(new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				event.acceptTransferModes(TransferMode.LINK);
				event.consume();
			}
		});
		
		template_choices.addListener(new ListChangeListener<String>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends String> c) {
				line_input.getItems().clear();
				line_input.getItems().addAll(template_choices);
			}
		});
		
		add_files_list.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {

				boolean success = false;

				try {

					List<File> draged_files = event.getDragboard().getFiles();
					
					if(draged_files.isEmpty()) {
						
						success = false;
						event.setDropCompleted(success);
						event.consume();
					}
					
					ListIterator<File> iter = draged_files.listIterator();

					while (iter.hasNext()) {
						
						File file = iter.next();
						
						if(file.isFile()) {
							String path = file.getAbsolutePath();
							if (!raw_files.contains(path))
								raw_files.add(path);
						}
					}

					if(settings.sort)
						raw_files.sort(null);
					
					construct_script();

					add_files_list.setItems(processed_files);
					
					if(processed_files.isEmpty()) {
						drag_text.setVisible(true);
						save_script_btn.setDisable(true);
					} else {
						drag_text.setVisible(false);
						save_script_btn.setDisable(false);
					}

				} catch (Exception e) {

					Alert alert = new Alert(AlertType.ERROR, "Drag and drop error!\n\n" + e.toString());
					alert.showAndWait();
				}

				success = true;
				event.setDropCompleted(success);
				event.consume();
			}
		});

		add_files_list.setOnDragEntered(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {

				drag_text.setVisible(false);
				event.consume();
			}
		});

		add_files_list.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {

				if(processed_files.isEmpty())
					drag_text.setVisible(true);
				else
					drag_text.setVisible(false);
				
				event.consume();
			}
		});

		add_files_list.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY && !processed_files.isEmpty()) {
					context_menu.show(add_files_list, event.getScreenX(), event.getScreenY());
					selected_item = add_files_list.getSelectionModel().getSelectedIndex();
				}
			}
		});

		add_files_list.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					processed_files.remove(add_files_list.getSelectionModel().getSelectedIndex());
					if (processed_files.isEmpty())
						drag_text.setVisible(true);
				}
			}
		});

		menu_remove.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				raw_files.remove(selected_item);
				processed_files.remove(selected_item);
				if (raw_files.isEmpty()) {
					drag_text.setVisible(true);
					save_script_btn.setDisable(true);
				}
			}
		});

		menu_clear.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				raw_files.clear();
				processed_files.clear();
				drag_text.setVisible(true);
				save_script_btn.setDisable(true);
			}
		});

		line_input.setOnKeyReleased(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				construct_script();
			}
		});

		line_input.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				construct_script();
			}
		});
		
		save_btn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				try {
					
					if(	line_input.getValue() == null || 
							line_input.getValue().equals(line_input.promptTextProperty()))
								return;
						
						if(!template_choices.contains(line_input.getValue()))
							template_choices.add(line_input.getValue());
					
				} catch (Exception e) {
					
					Alert alert = new Alert(AlertType.ERROR, "Script template save error!\n\n" + e.toString());
					alert.showAndWait();
					return;
				}
				
				save();
			}
		});
		
		delete_btn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				try {
					
					template_choices.remove(line_input.getValue());
					line_input.setValue(null);
					
				} catch (Exception e) {
					
					Alert alert = new Alert(AlertType.ERROR, "Script template delete error!\n\n" + e.toString());
					alert.showAndWait();
					return;
				}

				save();
			}
		});
		
		save_script_btn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				
				try {
					
					FileChooser fc = new FileChooser();
					fc.getExtensionFilters().add(new ExtensionFilter("Text file", "*.txt"));
					fc.getExtensionFilters().add(new ExtensionFilter("AutoCAD script", "*.scr"));
					fc.getExtensionFilters().add(new ExtensionFilter("All files", "*.*"));
					
					if(settings.lastSavePath != null) {
						
						File pathFile = new File(settings.lastSavePath);
						fc.setInitialDirectory(pathFile);
						pathFile.delete();
					}
					
					File file = fc.showSaveDialog(null);
					
					if (file != null) {
						
						settings.lastSavePath = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf('\\') + 1);
						
						FileWriter writer = new FileWriter(file);
						String separator = System.getProperty("line.separator");
						
						for (String string : processed_files) {
							writer.append(string + separator);
						}

						writer.close();
					}
					
				} catch (Exception e) {
					
					Alert alert = new Alert(AlertType.ERROR, "Script save error!\n\n" + e.toString());
					alert.showAndWait();
					return;
				}

				save();
			}
		});
	}

	private void construct_script() {

		try {

			ListIterator<String> iter = raw_files.listIterator();

			processed_files.clear();

			while (iter.hasNext()) {

				String path_file_ext = iter.next();

				String path = path_file_ext.substring(0, path_file_ext.lastIndexOf('\\'));
				String path_file = path_file_ext.substring(0, path_file_ext.lastIndexOf('.'));
				String file_ext = path_file_ext.substring(path_file_ext.lastIndexOf('\\') + 1, path_file_ext.length());
				String file = file_ext.substring(0, file_ext.lastIndexOf('.'));

				String script_template = line_input.getValue();
				if(script_template == null)
					script_template = "<PATH+FILE+EXTENSION>";

				script_template = script_template.replace("<PATH+FILE+EXTENSION>", path_file_ext);
				script_template = script_template.replace("<PATH+FILE>", path_file);
				script_template = script_template.replace("<FILE+EXTENSION>", file_ext);
				script_template = script_template.replace("<FILE>", file);
				script_template = script_template.replace("<PATH>", path);

				processed_files.add(script_template);

			}

		} catch (Exception e) {

			Alert alert = new Alert(AlertType.ERROR, "Script construction error!\n\n" + e.toString());
			alert.showAndWait();
		}
	}

	private void load() {

		try {

			Scanner reader = new Scanner(new File("cmds.txt"));

			while (reader.hasNextLine())
				template_choices.add(reader.nextLine());

			reader.close();
			line_input.getItems().addAll(template_choices);

		} catch (FileNotFoundException e) {

			template_choices.addAll("<PATH+FILE+EXTENSION>", "<PATH+FILE>", "<FILE+EXTENSION>", "<FILE>", "<PATH>");
			line_input.getItems().addAll(template_choices);
			
		} catch (Exception e) {
			
			Alert alert = new Alert(AlertType.ERROR, "Script template load function error!\n\n" + e.toString());
			alert.showAndWait();
			return;
		}

	}

	private void save() {

		try {
			
			FileWriter writer = new FileWriter("cmds.txt");

			Iterator<String> iter = template_choices.iterator();

			while (iter.hasNext())
				writer.append(iter.next() + '\n');

			writer.close();

		} catch (Exception e) {

			Alert alert = new Alert(AlertType.ERROR, "Script template save function error!\n\n" + e.toString());
			alert.showAndWait();
			return;
		}

	}
}