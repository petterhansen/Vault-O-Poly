package mechanics;

import players.Player;

/**
 * Encapsulates the state of an ongoing trade negotiation.
 * Eliminates the need for multiple scattered pending variables.
 */
public class TradeSession {
    private Player requester;
    private Player partner;
    private TradeOffer requesterOffer;
    private TradeOffer partnerOffer;
    private boolean requesterResponseReceived;
    private boolean partnerResponseReceived;
    private boolean requesterAccepted;
    private boolean partnerAccepted;

    public TradeSession(Player requester, Player partner) {
        this.requester = requester;
        this.partner = partner;
        this.requesterOffer = new TradeOffer();
        this.partnerOffer = new TradeOffer();
        this.requesterResponseReceived = false;
        this.partnerResponseReceived = false;
        this.requesterAccepted = false;
        this.partnerAccepted = false;
    }

    public Player getRequester() { return requester; }
    public Player getPartner() { return partner; }

    public TradeOffer getRequesterOffer() { return requesterOffer; }
    public void setRequesterOffer(TradeOffer offer) { this.requesterOffer = offer; }

    public TradeOffer getPartnerOffer() { return partnerOffer; }
    public void setPartnerOffer(TradeOffer offer) { this.partnerOffer = offer; }

    public void setRequesterResponse(boolean accepted) {
        this.requesterResponseReceived = true;
        this.requesterAccepted = accepted;
    }

    public void setPartnerResponse(boolean accepted) {
        this.partnerResponseReceived = true;
        this.partnerAccepted = accepted;
    }

    public boolean bothResponsesReceived() {
        return requesterResponseReceived && partnerResponseReceived;
    }

    public boolean bothAccepted() {
        return requesterAccepted && partnerAccepted;
    }

    public boolean isPlayerInvolved(Player player) {
        return player.equals(requester) || player.equals(partner);
    }

    public String getSummary() {
        String p1Name = requester.getName();
        String p2Name = partner.getName();
        return "--- THE PROPOSED DEAL ---\n\n" +
                p1Name + " offers:\n" +
                requesterOffer.getSummary() + "\n" +
                p2Name + " offers:\n" +
                partnerOffer.getSummary() + "\n" +
                "Do you accept this deal?";
    }
}