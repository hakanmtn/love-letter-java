package loveletter.viewmodel;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;


/**
 * Processes nickname input and provides feedback for the login view.
 */
public class LoginViewModel {


    private final ReadOnlyStringWrapper feedback = new ReadOnlyStringWrapper("");

    /**
     * Creates a view model with empty feedback.
     */
    public LoginViewModel(){

    }

    /**
     * Validates the nickname and updates the feedback.
     * This method does not connect to the server.
     *
     * @param input the nickname input; may be null
     */
    public void confirmNickname(String input){
        String nickname = input == null ? "" : input.trim();

        if (nickname.isBlank()){
            feedback.set("Please enter a nickname.");
        }else {
            feedback.set("Your nickname: " + nickname);
        }
    }

    /**
     * Returns the observable feedback for the view.
     *
     * @return the read-only feedback property
     */
    public ReadOnlyStringProperty feedbackProperty(){
        return feedback.getReadOnlyProperty();
    }
}
