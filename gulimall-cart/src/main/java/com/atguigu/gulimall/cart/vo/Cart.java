package com.atguigu.gulimall.cart.vo;
import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 * 需要计算的属性，必须重写他的get方法，保证每次获取属性都会进行计算
 */
public class Cart {
    private List<CartItem> items;

    private Integer countNum; // product number

    private Integer countType; // product type count

    private BigDecimal totalAmount; // product total price

    private BigDecimal reduce = new BigDecimal("0"); //reduced price

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int countNum = 0;
        if (this.items != null&& items.size() > 0) {
            for (CartItem item : items) {
                countNum += item.getCount();
            }
        }
        return countNum;
    }

    public void setCountNum(Integer countNum) {
        this.countNum = countNum;
    }

    public Integer getCountType() {
        int count = 0;
        if (this.items != null&& items.size() > 0) {
            for (CartItem item : items) {
                count += 1;
            }
        }
        return count;
    }

    public void setCountType(Integer countType) {
        this.countType = countType;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        // 计算购物项总价
        if (this.items != null&& items.size() > 0) {
            for (CartItem item : items) {
                BigDecimal totalPrice = item.getTotalPrice();
                amount = amount.add(totalPrice);
            }
        }
        // 减去优惠总价
        amount =amount.subtract(getReduce());

        return amount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
