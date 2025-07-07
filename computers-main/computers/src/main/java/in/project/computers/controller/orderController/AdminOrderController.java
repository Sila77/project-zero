package in.project.computers.controller.orderController;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.service.orderService.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// หมายเหตุ: หากต้องการเพิ่ม Endpoint สำหรับ Admin ที่ดึงข้อมูล Order หลายๆ รายการ
// ควรพิจารณาใช้ Pageable เพื่อจัดการ Pagination
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.Pageable;

/**
 * <h3>Admin Order Controller</h3>
 * <p>
 * Controller สำหรับจัดการ API Endpoints ทั้งหมดที่เกี่ยวกับการจัดการ Order ซึ่งต้องใช้สิทธิ์ของผู้ดูแลระบบ (Admin)
 * ทุก Endpoint ใน Controller นี้จะถูกป้องกันโดย `@PreAuthorize("hasRole('ADMIN')")`
 * ซึ่งหมายความว่าผู้ที่เรียก API ต้องมี Role 'ADMIN' ในระบบเท่านั้น
 * </p>
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // บังคับว่าทุก Endpoint ในนี้ต้องใช้สิทธิ์ Admin
public class AdminOrderController {

    private final OrderService orderService;
    // หมายเหตุ: ในอนาคตอาจจะต้องสร้าง AdminOrderService แยกต่างหาก
    // เพื่อจัดการ Logic ที่ซับซ้อนและไม่ควรปนกับ User-facing service

    /**
     * <h4>[POST] /api/admin/orders/approve-refund/{orderId}</h4>
     * <p>Endpoint สำหรับอนุมัติคำขอคืนเงินที่ผู้ใช้ส่งเข้ามา</p>
     * <p><b>การทำงาน:</b></p>
     * <ul>
     *     <li>หากเป็นการชำระเงินผ่าน PayPal: จะเรียก API ของ PayPal เพื่อทำการคืนเงินจริง</li>
     *     <li>หากเป็นการโอนเงิน: จะอัปเดตสถานะให้ Admin ไปดำเนินการโอนคืนเอง</li>
     *     <li>จากนั้นจะทำการคืนสต็อกสินค้า และอัปเดตสถานะ Order เป็น REFUNDED</li>
     * </ul>
     * <p><b>ตัวอย่างการเรียก:</b> {@code POST /api/admin/orders/approve-refund/ord_123456789}</p>
     *
     * @param orderId ID ของ Order ที่จะอนุมัติการคืนเงิน
     * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะอัปเดตเป็น REFUNDED
     */
    @PostMapping("/approve-refund/{orderId}")
    public ResponseEntity<OrderResponse> approveRefund(@PathVariable String orderId) {
        try {
            log.info("Admin action: Approving refund for order ID: {}", orderId);
            OrderResponse response = orderService.approveRefund(orderId);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            log.error("Admin action: Error processing PayPal refund for order ID: {}. Error: {}", orderId, e.getMessage());
            // ส่งคืนข้อผิดพลาดที่เฉพาะเจาะจงมากขึ้นเกี่ยวกับ PayPal
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing PayPal refund: " + e.getMessage(), e);
        }
    }

    /**
     * <h4>[POST] /api/admin/orders/reject-refund/{orderId}</h4>
     * <p>Endpoint สำหรับปฏิเสธคำขอคืนเงินที่ผู้ใช้ส่งเข้ามา</p>
     * <p><b>การทำงาน:</b> จะทำการอัปเดตสถานะ Order เป็น REFUND_REJECTED</p>
     * <p><b>ตัวอย่างการเรียก:</b> {@code POST /api/admin/orders/reject-refund/ord_123456789}</p>
     *
     * @param orderId ID ของ Order ที่จะปฏิเสธการคืนเงิน
     * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะอัปเดตเป็น REFUND_REJECTED
     */
    @PostMapping("/reject-refund/{orderId}")
    public ResponseEntity<OrderResponse> rejectRefund(@PathVariable String orderId) {
        log.info("Admin action: Rejecting refund for order ID: {}", orderId);
        OrderResponse response = orderService.rejectRefund(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * <h4>[GET] /api/admin/orders/{orderId}</h4>
     * <p>Endpoint สำหรับดูรายละเอียด Order ใดๆ ก็ได้ในระบบ</p>
     * <p><b>การทำงาน:</b></p>
     * <ul>
     *   <li>เมธอดนี้ถูกสร้างขึ้นเพื่อ Admin โดยเฉพาะ ทำให้สามารถดูข้อมูล Order ของผู้ใช้คนไหนก็ได้</li>
     *   <li>ในอนาคต ควรสร้างเมธอดใน Service แยกสำหรับ Admin ที่ไม่ตรวจสอบความเป็นเจ้าของ (Bypass ownership check)</li>
     * </ul>
     * <p><b>ตัวอย่างการเรียก:</b> {@code GET /api/admin/orders/ord_123456789}</p>
     *
     * @param orderId ID ของ Order ที่ต้องการดูรายละเอียด
     * @return ResponseEntity ที่มีรายละเอียดทั้งหมดของ Order ที่ร้องขอ
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getAnyOrderById(@PathVariable String orderId) {
        log.info("Admin action: Fetching order details for ID: {}", orderId);

        // หมายเหตุ: การเรียก orderService.getOrderById(orderId) ในปัจจุบันยังมีการตรวจสอบสิทธิ์เจ้าของ
        // หากต้องการให้ Admin ดูได้ทุก Order จริงๆ จะต้องสร้างเมธอดใหม่ใน Service เช่น
        // `getAnyOrderByIdForAdmin(orderId)` ที่ไม่มีการเช็ค `order.getUserId().equals(currentUserId)`
        // อย่างไรก็ตาม เพื่อความง่าย จะใช้เมธอดเดิมไปก่อน ซึ่งจะทำให้ Admin ดูได้เฉพาะ Order ที่ตัวเองสร้าง (ถ้ามี)

        // นี่คือโค้ดที่ "ควรจะเป็น" ในอนาคต (สมมติว่ามีเมธอด `getAnyOrderByIdForAdmin` แล้ว)
        // OrderResponse response = adminOrderService.getAnyOrderByIdForAdmin(orderId);

        // ใช้โค้ดปัจจุบันไปก่อน
        OrderResponse response = orderService.getOrderById(orderId); // <<< อาจต้องแก้ไขใน Service

        return ResponseEntity.ok(response);
    }

    /*
     * ==========================================================
     *  ตัวอย่าง Endpoints เพิ่มเติมที่สามารถสร้างได้ในอนาคต
     * ==========================================================
     */

    // /**
    //  * <h4>[GET] /api/admin/orders</h4>
    //  * <p>Endpoint สำหรับดึงรายการ Order ทั้งหมดในระบบ พร้อมระบบแบ่งหน้า (Pagination)</p>
    //  * <p><b>ตัวอย่างการเรียก:</b> {@code GET /api/admin/orders?page=0&size=20&sort=createdAt,desc}</p>
    //  * @param pageable ตัวแปรสำหรับจัดการ Pagination และการเรียงลำดับ
    //  * @return Page<OrderResponse> ที่มีข้อมูล Order ในหน้านั้นๆ และข้อมูลเกี่ยวกับจำนวนหน้าทั้งหมด
    //  */
    // @GetMapping
    // public ResponseEntity<Page<OrderResponse>> getAllOrders(Pageable pageable) {
    //     // ต้องสร้างเมธอด `getAllOrders(pageable)` ใน Service เพิ่ม
    //     Page<OrderResponse> orders = orderService.getAllOrders(pageable);
    //     return ResponseEntity.ok(orders);
    // }

    // /**
    //  * <h4>[POST] /api/admin/orders/approve-slip/{orderId}</h4>
    //  * <p>Endpoint สำหรับอนุมัติสลิปโอนเงินที่ผู้ใช้ส่งมา</p>
    //  * <p><b>ตัวอย่างการเรียก:</b> {@code POST /api/admin/orders/approve-slip/ord_abcdef123}</p>
    //  * @param orderId ID ของ Order ที่จะอนุมัติสลิป
    //  * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะอัปเดตเป็น PROCESSING
    //  */
    // @PostMapping("/approve-slip/{orderId}")
    // public ResponseEntity<OrderResponse> approvePaymentSlip(@PathVariable String orderId) {
    //     // ต้องสร้างเมธอด `approvePaymentSlip(orderId)` ใน Service เพิ่ม
    //     OrderResponse response = orderService.approvePaymentSlip(orderId);
    //     return ResponseEntity.ok(response);
    // }
}