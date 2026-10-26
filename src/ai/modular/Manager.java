package ai.modular;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;

public interface Manager {
    void act(int player, Player p, GameState gs, PhysicalGameState pgs, Blackboard bb);
}
