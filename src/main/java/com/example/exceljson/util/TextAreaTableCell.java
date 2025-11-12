package com.example.exceljson.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * A custom TableCell that uses a TextArea for editing, allowing multiline text input.
 * Supports Shift+Enter to insert newlines, and plain Enter to commit the edit.
 */
public class TextAreaTableCell<S> extends TableCell<S, String> {

    private TextArea textArea;
    private final StringConverter<String> converter;

    public TextAreaTableCell() {
        this(new DefaultStringConverter());
    }

    public TextAreaTableCell(StringConverter<String> converter) {
        this.converter = converter;
        this.getStyleClass().add("text-area-table-cell");
    }

    /**
     * Creates a cell factory for use in TableColumn.
     */
    public static <S> Callback<javafx.scene.control.TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());
    }

    /**
     * Creates a cell factory with a custom StringConverter.
     */
    public static <S> Callback<javafx.scene.control.TableColumn<S, String>, TableCell<S, String>> forTableColumn(
            StringConverter<String> converter) {
        return list -> new TextAreaTableCell<>(converter);
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }

        super.startEdit();

        if (isEditing()) {
            if (textArea == null) {
                createTextArea();
            }

            textArea.setText(getItemText());
            setText(null);
            setGraphic(textArea);
            textArea.selectAll();
            textArea.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItemText());
        setGraphic(null);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textArea != null) {
                    textArea.setText(getItemText());
                }
                setText(null);
                setGraphic(textArea);
            } else {
                setText(getItemText());
                setGraphic(null);
            }
        }
    }

    private void createTextArea() {
        textArea = new TextArea();
        textArea.setMinHeight(60);
        textArea.setPrefRowCount(3);
        textArea.setWrapText(true);

        // Handle key events
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    // Shift+Enter: Insert newline
                    event.consume();
                    int caretPosition = textArea.getCaretPosition();
                    textArea.insertText(caretPosition, "\n");
                } else {
                    // Plain Enter: Commit the edit
                    event.consume();
                    commitEdit(converter.fromString(textArea.getText()));
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                // Escape: Cancel the edit
                event.consume();
                cancelEdit();
            } else if (event.getCode() == KeyCode.TAB) {
                // Tab: Commit and move to next cell
                event.consume();
                commitEdit(converter.fromString(textArea.getText()));
                TableCell<S, String> cell = TextAreaTableCell.this;
                cell.getTableView().getSelectionModel().selectNext();
                cell.getTableView().edit(
                    cell.getTableView().getSelectionModel().getSelectedIndex(),
                    cell.getTableColumn()
                );
            }
        });

        // Auto-commit when focus is lost
        textArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (wasFocused && !isNowFocused) {
                if (isEditing()) {
                    commitEdit(converter.fromString(textArea.getText()));
                }
            }
        });
    }

    private String getItemText() {
        return getItem() == null ? "" : converter.toString(getItem());
    }
}
