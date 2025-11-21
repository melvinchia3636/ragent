package dev.ragent.view;

import dev.ragent.model.ChatMessage;
import dev.ragent.model.ContextReference;
import dev.ragent.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.List;

/**
 * Dialog to display message details including context references and query
 * variations
 */
public class MessageDetailsDialog extends BaseDialog {

    @FXML
    private TabPane tabPane;

    @FXML
    private VBox contextContainer;

    @FXML
    private VBox variationsContainer;

    private final ChatMessage message;

    public MessageDetailsDialog(Stage owner, ChatMessage message) {
        super(owner, "Message Details", "/dev/ragent/message_details.fxml", true);
        this.message = message;

        // Set dialog size
        getStage().setWidth(700);
        getStage().setHeight(600);

        loadMessageData();
    }

    /**
     * Load message data from database and populate the UI
     */
    private void loadMessageData() {
        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            showError("Database unavailable");
            return;
        }

        // Load context references
        List<ContextReference> contexts = databaseService.getMessageContexts(message.getId());
        message.setContextReferences(contexts);

        // Load query variations
        List<String> variations = databaseService.getQueryVariations(message.getId());
        message.setQueryVariations(variations);

        // Populate UI
        populateContextReferences();
        populateQueryVariations();
    }

    /**
     * Populate the context references tab
     */
    private void populateContextReferences() {
        contextContainer.getChildren().clear();

        List<ContextReference> contexts = message.getContextReferences();

        if (contexts == null || contexts.isEmpty()) {
            Label emptyLabel = new Label("No context references available for this message.");
            emptyLabel.getStyleClass().add("empty-state-message");
            contextContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < contexts.size(); i++) {
            ContextReference context = contexts.get(i);
            VBox contextCard = createContextCard(i + 1, context);
            contextContainer.getChildren().add(contextCard);
        }
    }

    /**
     * Create a card for a single context reference
     */
    private VBox createContextCard(int index, ContextReference context) {
        VBox card = new VBox(8);
        card.getStyleClass().add("context-card");

        // Header with index, file name, and score
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label indexLabel = new Label("#" + index);
        indexLabel.getStyleClass().add("context-index");

        Label fileLabel = new Label(context.getFileName());
        fileLabel.getStyleClass().add("context-file");
        HBox.setHgrow(fileLabel, Priority.ALWAYS);

        Label scoreLabel = new Label(context.getFormattedScore());
        scoreLabel.getStyleClass().add("context-score");

        header.getChildren().addAll(indexLabel, fileLabel, scoreLabel);

        // Chunk text
        Text chunkText = new Text(context.getChunkText());
        chunkText.getStyleClass().add("context-chunk");
        TextFlow chunkFlow = new TextFlow(chunkText);
        chunkFlow.getStyleClass().add("context-chunk");
        chunkFlow.maxWidthProperty().bind(card.widthProperty().subtract(40)); // Account for padding

        // File path (full path)
        if (!context.getFilePath().equals(context.getFileName())) {
            Text pathText = new Text(context.getFilePath());
            pathText.getStyleClass().add("context-path");
            TextFlow pathFlow = new TextFlow(pathText);
            pathFlow.getStyleClass().add("context-path");
            pathFlow.maxWidthProperty().bind(card.widthProperty().subtract(40)); // Account for padding
            card.getChildren().addAll(header, chunkFlow, pathFlow);
        } else {
            card.getChildren().addAll(header, chunkFlow);
        }

        return card;
    }

    /**
     * Populate the query variations tab
     */
    private void populateQueryVariations() {
        variationsContainer.getChildren().clear();

        List<String> variations = message.getQueryVariations();

        if (variations == null || variations.isEmpty()) {
            Label emptyLabel = new Label("No query variations available for this message.");
            emptyLabel.getStyleClass().add("empty-state-message");
            variationsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < variations.size(); i++) {
            String variation = variations.get(i);
            VBox variationCard = createVariationCard(i + 1, variation);
            variationsContainer.getChildren().add(variationCard);
        }
    }

    /**
     * Create a card for a single query variation
     */
    private VBox createVariationCard(int index, String variation) {
        VBox card = new VBox(4);
        card.getStyleClass().add("variation-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label indexLabel = new Label("Variation " + index);
        indexLabel.getStyleClass().add("variation-index");

        if (index == 1) {
            Label originalBadge = new Label("Original");
            originalBadge.getStyleClass().add("variation-badge");
            header.getChildren().addAll(indexLabel, originalBadge);
        } else {
            header.getChildren().add(indexLabel);
        }

        Text variationText = new Text(variation);
        variationText.getStyleClass().add("variation-text");
        TextFlow variationFlow = new TextFlow(variationText);
        variationFlow.getStyleClass().add("variation-text");
        variationFlow.maxWidthProperty().bind(card.widthProperty().subtract(40)); // Account for padding

        card.getChildren().addAll(header, variationFlow);

        return card;
    }

    /**
     * Show error message in the dialog
     */
    private void showError(String message) {
        Label errorLabel = new Label("Error: " + message);
        errorLabel.getStyleClass().add("error-message");
        contextContainer.getChildren().add(errorLabel);
        variationsContainer.getChildren().add(new Label("Error: " + message));
    }

    @FXML
    private void handleClose() {
        close();
    }
}
