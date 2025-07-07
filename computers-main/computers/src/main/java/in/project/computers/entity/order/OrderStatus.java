package in.project.computers.entity.order;

public enum OrderStatus {
    PENDING_PAYMENT,  // รอการชำระเงิน
    PROCESSING,       // กำลังดำเนินการ (จ่ายเงินแล้ว)
    SHIPPED,          // จัดส่งแล้ว
    COMPLETED,        // ส่งถึงมือลูกค้าแล้ว
    CANCELLED,        // ยกเลิกแล้ว

    REFUND_REQUESTED, // ผู้ใช้ส่งคำขอคืนเงิน
    REFUNDED,         // คืนเงินสำเร็จแล้ว
    REFUND_REJECTED   // แอดมินปฏิเสธคำขอคืนเงิน
}