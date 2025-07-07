package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.PaymentDetails;
import in.project.computers.entity.order.PaymentMethod;
import in.project.computers.entity.user.UserEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * <h3>Order Helper Service Interface (ฉบับสมบูรณ์)</h3>
 * <p>
 * Interface นี้กำหนด "สัญญา" สำหรับคลาสผู้ช่วย (Helper) ที่จะเข้ามาจัดการ
 * Logic ที่ซับซ้อนในการประมวลผล Order
 * </p>
 * <p>
 * <b>การเปลี่ยนแปลงสำคัญ:</b>
 * <ul>
 *   <li>เมธอด <code>unpackAndAggregateItems</code> ถูกลบออกไป เนื่องจากเป็น Logic ภายในที่ไม่ควรเป็นส่วนหนึ่งของ Public Interface</li>
 *   <li>เมธอด <code>createAndValidateBaseOrder</code> ถูกปรับ Signature ใหม่ให้รับแค่ <code>CreateOrderRequest</code> และ <code>UserEntity</code> ซึ่งสะท้อนการทำงานของ Implementation ใหม่ที่เรียบง่ายขึ้น</li>
 * </ul>
 * </p>
 */
public interface OrderHelperService {

    // --- โซนที่ 1: การสร้างและตรวจสอบความถูกต้องของ Order (Order Creation & Validation) ---

    /**
     * สร้าง {@link Order} object ใน Memory โดยอ่านข้อมูลโดยตรงจาก Request
     * พร้อมตรวจสอบความถูกต้องทั้งหมด (เช่น เช็คสต็อก, คำนวณยอดรวม)
     * และรักษารูปแบบของ "Build" และ "Component" แยกจากกัน
     *
     * @param request     อ็อบเจกต์คำสั่งซื้อจากผู้ใช้ (DTO ที่ปรับปรุงใหม่)
     * @param currentUser Entity ของผู้ใช้ที่กำลังสั่งซื้อ
     * @return {@link Order} object ที่สมบูรณ์และผ่านการตรวจสอบแล้ว พร้อมสำหรับบันทึกลงฐานข้อมูล
     */
    Order createAndValidateBaseOrder(CreateOrderRequest request, UserEntity currentUser);

    // --- โซนที่ 2: การจัดการสต็อกสินค้า (Stock Management) ---

    /**
     * ตัดสต็อกสินค้าในคลัง (Inventory) หลังจากชำระเงินสำเร็จ
     * โดยจะวนลูปตามโครงสร้างของ OrderLineItem ที่มีทั้ง Build และ Component
     *
     * @param order Order ที่การชำระเงินสำเร็จและต้องการตัดสต็อก
     */
    void decrementStockForOrder(Order order);

    /**
     * เพิ่มสต็อกสินค้าในคลัง (Inventory) หลังจากอนุมัติการคืนเงิน (Refund)
     * โดยจะวนลูปตามโครงสร้างของ OrderLineItem ที่มีทั้ง Build และ Component
     *
     * @param order Order ที่ได้รับการอนุมัติคืนเงินและต้องการคืนสต็อก
     */
    void incrementStockForOrder(Order order);

    // --- โซนที่ 3: การจัดการการชำระเงิน (Payment Processing Helpers) ---

    /**
     * ดำเนินการ Refund ผ่าน PayPal API สำหรับ Order ที่ระบุ
     *
     * @param order          Order ที่ต้องการ Refund ผ่าน PayPal
     * @param paymentDetails PaymentDetails ของ Order นั้น
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อ PayPal
     */
    void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException;

    /**
     * ดึง Sale ID จาก PayPal Payment object ที่ได้รับมา
     *
     * @param originalPaypalPayment PayPal Payment object ที่ได้จากการดึงข้อมูลด้วย Payment ID เดิม
     * @param orderIdForLog         ID ของ Order (สำหรับ Logging และ Error message)
     * @return String ของ Sale ID
     * @throws ResponseStatusException หากไม่พบ Sale ID
     */
    String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog);


    // --- โซนที่ 4: การค้นหาและตรวจสอบ Order (Order Retrieval & Processing Checks) ---

    /**
     * ค้นหา Order จาก ID และตรวจสอบสิทธิ์, ความถูกต้องของ PaymentMethod และสถานะ Payment
     * ก่อนนำไปประมวลผล
     *
     * @param orderId        ID ของ Order ที่ต้องการค้นหา
     * @param userId         ID ของผู้ใช้ปัจจุบันที่กำลังทำรายการ
     * @param expectedMethod วิธีการชำระเงินที่คาดหวังสำหรับ Action นี้
     * @return {@link Order} object ที่ผ่านการตรวจสอบทุกขั้นตอนแล้ว
     */
    Order findOrderForProcessing(String orderId, String userId, PaymentMethod expectedMethod);

    // --- โซนที่ 5: การแปลงข้อมูล (Data Transformation) ---

    /**
     * แปลง {@link Order} Entity ไปเป็น {@link OrderResponse} DTO
     *
     * @param order Entity ที่ต้องการแปลง
     * @return DTO ที่พร้อมส่งกลับไปยัง Client
     */
    OrderResponse entityToResponse(Order order);
}