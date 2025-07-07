// in/project/computers/entity/Inventory.java
package in.project.computers.entity.component;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;

@Data
@Builder
@Document(collection = "inventories")
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {
    @Id
    private String id;

    @Indexed(unique = true)
    private String componentId;

    @Min(0)
    private int quantity;

    private BigDecimal price;
}