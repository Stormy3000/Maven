package io.hotmoka.examples.wine.resources;

import io.hotmoka.examples.wine.staff.Authority;
import io.hotmoka.examples.wine.staff.Role;
import io.hotmoka.examples.wine.staff.SupplyChain;
import io.hotmoka.examples.wine.staff.Worker;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

import static io.takamaka.code.lang.Takamaka.require;

public final class Wine extends Resource {
    private String unitOfMeasure = "l";
    private boolean eligible = false;

    @FromContract
    public Wine(SupplyChain chain, String name, String description, int amount, Resource origin) {
        super(chain, name, description, amount, origin);
        require(((Worker) caller()).getRole() == Role.WINE_MAKING_CENTRE,
                "Only a Wine-Making Centre can create a new object Wine.");
    }

    @View
    public boolean isEligible() {
        return eligible;
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void setUnitOfMeasure(String unitOfMeasure, Worker wine_making_centre) {
        require(producers.contains(wine_making_centre) && wine_making_centre.getRole() == Role.WINE_MAKING_CENTRE,
                "Only a Worker who produced the wine can set the unit of measure.");
        this.unitOfMeasure = unitOfMeasure;
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void setEligible(Authority authority) {
        require(chain.getAuthorities().contains(authority), "Only an Authority can say Wine is eligible.");
        eligible = true;
    }
}
