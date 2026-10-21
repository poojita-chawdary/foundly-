import {
  CreditCard,
  Headphones,
  Backpack,
  BookOpen,
  KeyRound,
  Package,
} from "lucide-react";
const categoryIcons = [
  CreditCard,
  Headphones,
  Backpack,
  BookOpen,
  KeyRound,
  Package,
];
export function CategoryIcon({
  name,
  size = 26,
}: {
  name: string;
  size?: number;
}) {
  const i = /card/i.test(name)
    ? 0
    : /electronic/i.test(name)
      ? 1
      : /bag/i.test(name)
        ? 2
        : /book/i.test(name)
          ? 3
          : /key/i.test(name)
            ? 4
            : 5;
  const Icon = categoryIcons[i];
  return <Icon size={size} strokeWidth={1.5} />;
}
