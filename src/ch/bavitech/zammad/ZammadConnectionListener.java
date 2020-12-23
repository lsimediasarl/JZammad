package ch.bavitech.zammad;

/**
 *
 * @author  sbodmer
 */
public interface ZammadConnectionListener {
   public void zammadReceivedResponse(String signature, ZammadResponse msg);
   
}
