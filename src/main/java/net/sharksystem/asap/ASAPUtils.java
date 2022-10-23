package net.sharksystem.asap;

import java.util.ArrayList;
import java.util.Collection;

public class ASAPUtils {
    /**
     *
     * @param searchSpace list of possible eras
     * @param fromEra lowest era
     * @param toEra highest era
     * @return list of era which are within from and to and also in search space
     */
    public static Collection<Integer> getErasInRange(Collection<Integer> searchSpace,
                                                     int fromEra, int toEra) {

        Collection<Integer> eras = new ArrayList<>();

        // the only trick is to be aware of the cyclic nature of era numbers
        boolean wrapped = fromEra > toEra; // it reached the era end and started new

        for(Integer era : searchSpace) {
            if(!wrapped) {
                //INIT ---- from-> +++++++++++++ <-to ----- MAX (+ fits)
                if(era >= fromEra && era <= toEra) eras.add(era);
            } else {
                // INIT+++++++++<-to ------ from->++++++MAX
                if(era <= toEra && era >= ASAP.INITIAL_ERA
                        || era >= fromEra && era <= ASAP.MAX_ERA
                ) eras.add(era);
            }
        }

        return eras;

    }
}
