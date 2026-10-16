package lavs.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

/** Builds the Name / Customer ID / Action rows shared by the customer tables. */
public final class CustomerRows {

    private CustomerRows() {
    }

    public static GridPane header() {
        GridPane row = baseRow(true);
        row.add(styled("Name", "table-header-cell"), 0, 0);
        row.add(styled("Customer ID", "table-header-cell"), 1, 0);
        row.add(styled("Action", "table-header-cell"), 2, 0);
        return row;
    }

    public static GridPane customer(String name, String customerNumber, Runnable onDetails) {
        GridPane row = baseRow(false);
        row.add(styled(name, "table-cell"), 0, 0);
        row.add(styled(customerNumber, "table-cell-muted"), 1, 0);

        Hyperlink details = new Hyperlink("Click here for more details");
        details.getStyleClass().add("link-details");
        details.setTranslateY(-3);
        details.setOnAction(e -> onDetails.run());
        row.add(details, 2, 0);
        return row;
    }

    private static GridPane baseRow(boolean header) {
        GridPane row = new GridPane();
        row.getStyleClass().add(header ? "table-header-row" : "table-row");
        row.setPadding(new Insets(0, 14, 0, 14));
        row.setMinHeight(header ? 30 : 38);
        row.setPrefHeight(header ? 30 : 38);
        row.setAlignment(Pos.CENTER_LEFT);

        for (double width : new double[] {42, 24, 34}) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            row.getColumnConstraints().add(column);
        }
        return row;
    }

    private static Label styled(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
