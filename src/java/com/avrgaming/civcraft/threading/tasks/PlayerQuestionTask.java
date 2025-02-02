/*
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.threading.tasks;


import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.questions.QuestionBaseTask;
import com.avrgaming.civcraft.questions.QuestionResponseInterface;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlayerQuestionTask extends QuestionBaseTask implements Runnable {

    Player askedPlayer; /* player who is being asked a question. */
    Player questionPlayer; /* player who has asked the question. */
    String question; /* Question being asked. */
    long timeout; /* Timeout after question expires. */
    //	RunnableWithArg finishedTask; /* Task to run when a response has been generated. */
    QuestionResponseInterface finishedFunction;

    protected String response = ""; /* Response to the question. */
    protected Boolean responded = false; /*Question was answered. */

    public PlayerQuestionTask() {
    }

    public PlayerQuestionTask(Player askedplayer, Player questionplayer, String question, long timeout,
                              QuestionResponseInterface finishedFunction) {

        this.askedPlayer = askedplayer;
        this.questionPlayer = questionplayer;
        this.question = question;
        this.timeout = timeout;
        this.finishedFunction = finishedFunction;

    }

    @Override
    public void run() {
        CivMessage.send(askedPlayer, ChatColor.GRAY + CivSettings.localize.localizedString("civleaderQtast_prompt1") + " " + ChatColor.AQUA + questionPlayer.getName());
        CivMessage.send(askedPlayer, String.valueOf(ChatColor.LIGHT_PURPLE) + ChatColor.BOLD + question);
        CivMessage.send(askedPlayer, ChatColor.GRAY + CivSettings.localize.localizedString("civleaderQtast_prompt2"));

        try {
            synchronized (this) {
                this.wait(timeout);
            }
        } catch (InterruptedException e) {
            cleanup();
            return;
        }

        if (responded) {
            finishedFunction.processResponse(response);
            cleanup();
            return;
        }

        CivMessage.send(askedPlayer, ChatColor.GRAY + CivSettings.localize.localizedString("var_PlayerQuestionTask_failedInTime", questionPlayer.getName()));
        CivMessage.send(questionPlayer, ChatColor.GRAY + CivSettings.localize.localizedString("var_civQtast_NoResponse", askedPlayer.getName()));
        cleanup();
    }

    public Boolean getResponded() {
        synchronized (responded) {
            return responded;
        }
    }

    public void setResponded(Boolean response) {
        synchronized (this.responded) {
            this.responded = response;
        }
    }

    public String getResponse() {
        synchronized (response) {
            return response;
        }
    }

    public void setResponse(String response) {
        synchronized (this.response) {
            setResponded(true);
            this.response = response;
        }
    }

    /* When this task finishes, remove itself from the hashtable. */
    private void cleanup() {
        CivGlobal.removeQuestion(askedPlayer.getName());
    }


}
