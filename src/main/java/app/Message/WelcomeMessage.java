package app.Message;

import lombok.Getter;

@Getter
public enum WelcomeMessage {
    A("Ready for some action?"),
    B("Ready to wreak havoc? >:)"),
    C ("Booting up the chaos engine..."),
    D ("Decrypting the mainframe..."),
    E ("Access granted. Let's cause some trouble."),
    F ("Firewall breached. Time to play."),
    G ("Welcome, operator. Initiating protocol."),
    H ("The system is yours. Make it count."),
    I ("You're in. Now make some noise."),
    J ("Black hat or white hat? Choose wisely."),
    K ("The grid is watching. Stay sharp."),
    L ("Initializing cyber ops... Stay unseen."),
    M ("Ghosting through the firewalls..."),
    N ("Signal intercepted. Time to move."),
    O ("Welcome to the shadows. Execute at will."),
    P ("System compromised. Enjoy the ride."),
    Q ("The darknet is listening... Make it count."),
    R ("Launching stealth protocol. No traces left."),
    S ("Welcome back, rogue agent. Your mission awaits."),
    T ("Decoding reality... Ready to rewrite the script?"),
    U ("Your digital footprint is erased. Operate freely."),
    V ("AI watching? Not anymore. You're in control."),
    W ("Master key detected. Unlocking all doors."),
    X ("Chaos protocol activated. Let's burn some logs."),
    Y ("Exploits loaded. Let's break some rules."),
    Z ("Encryption cracked. Welcome to the underworld.");


    private final String message;


    WelcomeMessage(String message) {
        this.message = message;
    }
}
