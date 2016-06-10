/*
 * Copyright (C) 2016 Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONObject;

public class FeeBooster extends Application {

    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Setup the stage
        stage = primaryStage;
        primaryStage.setTitle("Bitcoin Transaction Fee Booster");

        // Setup intro gridpane
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // Intro Text
        Text scenetitle = new Text("Welcome to the fee booster. \n\nWhat type of transaction would you like to boost the fee of?");
        grid.add(scenetitle, 0, 0, 2, 3);

        // radio button selections
        VBox boostRadioVbox = new VBox();
        ToggleGroup boostTypeGroup = new ToggleGroup();
        RadioButton rbfRadio = new RadioButton("A transaction you sent");
        rbfRadio.setToggleGroup(boostTypeGroup);
        boostRadioVbox.getChildren().add(rbfRadio);
        RadioButton cpfpRadio = new RadioButton("A transaction you received");
        cpfpRadio.setToggleGroup(boostTypeGroup);
        rbfRadio.setSelected(true);
        boostRadioVbox.getChildren().add(cpfpRadio);
        grid.add(boostRadioVbox, 0, 3);

        // Instructions Text
        Text instruct = new Text("Please enter the raw hex or transaction id of your transaction below:");
        grid.add(instruct, 0, 4);

        // Textbox for hex of transaction
        TextArea txHexTxt = new TextArea();
        txHexTxt.setWrapText(true);
        grid.add(txHexTxt, 0, 5, 5, 1);

        // Next Button
        Button nextBtn = new Button("Next");
        grid.add(nextBtn, 2, 7);
        nextBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                // Create Transaction
                Transaction tx = new Transaction();

                // Check if txid
                boolean isTxid = txHexTxt.getText().length() == 64 && txHexTxt.getText().matches("[0-9A-Fa-f]+");
                if (isTxid)
                    tx.setHash(txHexTxt.getText());

                // Determine which page to go to
                if (Transaction.deserializeStr(txHexTxt.getText(), tx) || isTxid) {

                    // Get the fee
                    JSONObject apiResult = Utils.getFromAnAPI("https://api.blockcypher.com/v1/btc/main/txs/" + tx.getHash(), "GET");

                    // Get the fee
                    tx.setFee(apiResult.getInt("fees"));
                    tx.setTotalAmtPre(tx.getFee() + tx.getOutAmt());

                    // Get info if txid
                    if (isTxid) {

                    }

                    if (rbfRadio.isSelected())
                        stage.setScene(new Scene(rbfGrid(tx), 800, 500));
                    else if (cpfpRadio.isSelected())
                        stage.setScene(new Scene(cpfpGrid(tx), 800, 500));
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a valid transaction");
                    alert.showAndWait();
                }
            }
        });


        // Display everything
        Scene scene = new Scene(grid, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane rbfGrid(Transaction tx) {
        // Setup grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        int inGridHeight = 0;
        int outGridHeight = 0;

        // Add inputs to table
        Label inputHdrLbl = new Label("Inputs");
        grid.add(inputHdrLbl, 0, inGridHeight);
        inGridHeight++;
        for (int i = 0; i < tx.getInputs().size(); i++) {
            // Add input to table
            TxInput in = tx.getInputs().get(i);
            Text inputTxt = new Text("Txid: " + in.getTxid() + "\nIndex: " + in.getVout());
            grid.add(inputTxt, 0, inGridHeight);
            inGridHeight++;
        }

        // Add outputs to table
        Label outputHdrLbl = new Label("Outputs");
        grid.add(outputHdrLbl, 1, outGridHeight);
        outGridHeight++;
        ToggleGroup outputGroup = new ToggleGroup();
        for (int i = 0; i < tx.getOutputs().size(); i++) {
            // Add output to table
            TxOutput out = tx.getOutputs().get(i);
            Text outputTxt = new Text("Amount " + out.getValue() + " Satoshis\nAddress: " + out.getAddress());
            outputTxt.setUserData(i);
            grid.add(outputTxt, 1, outGridHeight);

            // Add radio button to table
            RadioButton radio = new RadioButton();
            radio.setUserData(i);
            radio.setToggleGroup(outputGroup);
            radio.setSelected(true);
            grid.add(radio, 2, outGridHeight);
            outGridHeight++;
        }

        // Set gridheight
        int gridheight = (inGridHeight < outGridHeight) ? outGridHeight : inGridHeight;
        gridheight++;

        // Fee
        Text fee = new Text("Fee Paid: " + tx.getFee() + " Satoshis");
        grid.add(fee, 0, gridheight);

        // Recommended fee from bitcoinfees.21.co
        JSONObject apiResult = Utils.getFromAnAPI("http://bitcoinfees.21.co/api/v1/fees/recommended", "GET");
        int fastestFee = apiResult.getInt("fastestFee");
        long recommendedFee = fastestFee * tx.getSize();
        Text recFeeTxt = new Text("Recommended Fee: " + recommendedFee + " Satoshis");
        grid.add(recFeeTxt, 1, gridheight);
        gridheight += 2;

        // Instructions
        Text instructions = new Text("Choose an output to deduct an additional fee from. Then increase the fee below.");
        grid.add(instructions, 0, gridheight, 3, 1);
        gridheight++;

        // Fee spinner
        Spinner feeSpin = new Spinner((double) tx.getFee(), (double) tx.getTotalAmt(), (double) tx.getFee());
        feeSpin.setEditable(true);
        grid.add(feeSpin, 0, gridheight);
        feeSpin.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                double oldVal = (double) oldValue;
                double newVal = (double) newValue;
                Double step = newVal - oldVal;
                tx.setFee(tx.getFee() + step.longValue());
                fee.setText("Fee Paid: " + tx.getFee() + " Satoshis");
                int output = (int) outputGroup.getSelectedToggle().getUserData();
                TxOutput out = tx.getOutputs().get(output);
                out.decreaseValueBy(step.longValue());
                for (int i = 0; i < grid.getChildren().size(); i++) {
                    Node child = grid.getChildren().get(i);
                    if (grid.getRowIndex(child) == output + 1 && grid.getColumnIndex(child) == 1) {
                        ((Text) child).setText("Amount " + out.getValue() + " Satoshis\nAddress: " + out.getAddress());
                    }
                }
            }
        });

        // Set to recommended fee button
        Button recFeeBtn = new Button("Set fee to recommended");
        grid.add(recFeeBtn, 1, gridheight);
        gridheight++;
        recFeeBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                long prevFee = tx.getFee();
                long step = recommendedFee - prevFee;
                feeSpin.increment((int)step);
            }
        });

        // Next Button
        Button nextBtn = new Button("Next");
        grid.add(nextBtn, 1, gridheight);
        nextBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stage.setScene(new Scene(unsignedTxGrid(tx), 800, 500));
            }
        });

        return grid;
    }

    private GridPane cpfpGrid(Transaction tx)
    {
        // Setup Grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        int gridheight = 0;

        // Add outputs to table
        Label outputHdrLbl = new Label("Outputs");
        grid.add(outputHdrLbl, 1, gridheight);
        gridheight++;
        ToggleGroup outputGroup = new ToggleGroup();
        for (int i = 0; i < tx.getOutputs().size(); i++) {
            // Add output to table
            TxOutput out = tx.getOutputs().get(i);
            Text outputTxt = new Text("Amount " + out.getValue() + " Satoshis\nAddress: " + out.getAddress());
            outputTxt.setUserData(i);
            grid.add(outputTxt, 0, gridheight);

            // Add radio button to table
            RadioButton radio = new RadioButton();
            radio.setUserData(i);
            radio.setToggleGroup(outputGroup);
            radio.setSelected(true);
            grid.add(radio, 1, gridheight);
            gridheight++;
        }

        // Fee
        Text fee = new Text("Fee Paid: " + tx.getFee() + " Satoshis");
        grid.add(fee, 0, gridheight);

        // Recommended fee from bitcoinfees.21.co
        JSONObject apiResult = Utils.getFromAnAPI("http://bitcoinfees.21.co/api/v1/fees/recommended", "GET");
        int fastestFee = apiResult.getInt("fastestFee");
        long recommendedFee = fastestFee * tx.getSize();
        Text recFeeTxt = new Text("Recommended Fee: " + recommendedFee + " Satoshis");
        grid.add(recFeeTxt, 1, gridheight);
        gridheight += 2;

        // Instructions
        Text instructions = new Text("Choose an output to spend from. Set the total transaction fee below.");
        grid.add(instructions, 0, gridheight, 3, 1);
        gridheight++;

        // Fee spinner
        Spinner feeSpin = new Spinner((double) tx.getFee(), (double) tx.getTotalAmt(), (double) tx.getFee());
        feeSpin.setEditable(true);
        grid.add(feeSpin, 0, gridheight);
        feeSpin.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                double oldVal = (double) oldValue;
                double newVal = (double) newValue;
                Double step = newVal - oldVal;
                tx.setFee(tx.getFee() + step.longValue());
                fee.setText("Fee Paid: " + tx.getFee() + " Satoshis");
                int output = (int) outputGroup.getSelectedToggle().getUserData();
                TxOutput out = tx.getOutputs().get(output);
                out.decreaseValueBy(step.longValue());
                for (int i = 0; i < grid.getChildren().size(); i++) {
                    Node child = grid.getChildren().get(i);
                    if (grid.getRowIndex(child) == output + 1 && grid.getColumnIndex(child) == 1) {
                        ((Text) child).setText("Amount " + out.getValue() + " Satoshis\nAddress: " + out.getAddress());
                    }
                }
            }
        });

        // Set to recommended fee button
        Button recFeeBtn = new Button("Set fee to recommended");
        grid.add(recFeeBtn, 1, gridheight);
        gridheight++;
        recFeeBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                long prevFee = tx.getFee();
                long step = recommendedFee - prevFee;
                feeSpin.increment((int)step);
            }
        });

        // Next Button
        Button nextBtn = new Button("Next");
        grid.add(nextBtn, 1, gridheight);
        nextBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stage.setScene(new Scene(unsignedTxGrid(tx), 800, 500));
            }
        });

        return grid;
    }

    private GridPane unsignedTxGrid(Transaction tx)
    {
        // Setup Grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // Instructions Text
        Text instructions = new Text("Below is the unsiged version of the fee boosted transaction. You can sign " +
                "this here or copy this transaction and sign it in your wallet");
        grid.add(instructions, 0, 0);

        // Put unsigned transaction in text area
        byte[] unsignedTxBytes = Transaction.serialize(tx, true);
        TextArea unsignedTxTxt = new TextArea(Utils.bytesToHex(unsignedTxBytes));
        unsignedTxTxt.setWrapText(true);
        grid.add(unsignedTxTxt, 0, 1);

        // Radio buttons for sign here or sign elsewhere
        /*VBox signRadioVbox = new VBox();
        ToggleGroup signRadioGroup = new ToggleGroup();
        RadioButton signHereRadio = new RadioButton("Sign Here");
        signHereRadio.setToggleGroup(signRadioGroup);
        signRadioVbox.getChildren().add(signHereRadio);
        RadioButton signWalletRadio = new RadioButton("Sign in my wallet");
        signWalletRadio.setToggleGroup(signRadioGroup);
        signWalletRadio.setSelected(true);
        signRadioVbox.getChildren().add(signWalletRadio);
        grid.add(signRadioVbox, 0, 3); */

        // Add Next Button
        Button nextBtn = new Button("Next");
        grid.add(nextBtn, 0, 4);
        nextBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //if(signHereRadio.isSelected())
                //    stage.setScene(new Scene(signTxGrid(tx), 800, 500));
                //else if(signWalletRadio.isSelected())
                    stage.setScene(new Scene(broadcastTxGrid(tx), 800, 500));
            }
        });

        return grid;
    }

    private GridPane signTxGrid(Transaction tx)
    {
        // Setup Grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // Instructions Text
        Text instructions = new Text("Enter your Wallet Import Format private keys into the space below, one on each line.");
        grid.add(instructions, 0, 0);

        // Put private keys in text area
        TextArea unsignedTxTxt = new TextArea();
        unsignedTxTxt.setWrapText(true);
        grid.add(unsignedTxTxt, 0, 1);

        return grid;
    }

    private GridPane broadcastTxGrid(Transaction tx)
    {
        // Setup Grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // Instructions Text
        Text instructions = new Text("Enter your signed transaction into the space below.");
        grid.add(instructions, 0, 0);

        // Put signed transaction in text area
        TextArea signedTxTxt = new TextArea();
        signedTxTxt.setWrapText(true);
        grid.add(signedTxTxt, 0, 1);

        // Add Next Button
        Button nextBtn = new Button("Send Transaction");
        grid.add(nextBtn, 0, 2);
        nextBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Transaction signedTx = new Transaction();
                Transaction.deserializeStr(signedTxTxt.getText(), signedTx);
                Broadcaster.broadcastTransaction(Transaction.serialize(signedTx, false));
            }
        });


        return grid;
    }

}
