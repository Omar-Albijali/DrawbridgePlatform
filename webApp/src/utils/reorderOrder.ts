import type { Order, OrderItem } from '../types';
import { cartService } from '../services/cartService';

export interface ReorderToCartResult {
  addedItems: number;
  failedItems: number;
  failedProductNames: string[];
}

export async function reorderOrderToCart(retailerId: string, order: Order): Promise<ReorderToCartResult> {
  const orderItems = (order.items ?? []).filter((item: OrderItem) => item.quantity > 0);

  if (orderItems.length === 0) {
    return {
      addedItems: 0,
      failedItems: 0,
      failedProductNames: [],
    };
  }

  const results = await Promise.allSettled(
    orderItems.map((item: OrderItem) => cartService.addItem(retailerId, item.productId, item.quantity)),
  );

  const failedProductNames: string[] = [];
  let addedItems = 0;

  results.forEach((result: PromiseSettledResult<unknown>, index: number) => {
    if (result.status === 'fulfilled') {
      addedItems += 1;
      return;
    }

    failedProductNames.push(orderItems[index].productName);
  });

  return {
    addedItems,
    failedItems: failedProductNames.length,
    failedProductNames,
  };
}
