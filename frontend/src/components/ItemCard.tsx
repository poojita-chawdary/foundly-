import { Link } from "react-router-dom";
import { MapPin, CalendarDays, ArrowUpRight } from "lucide-react";
import type { Item } from "../api";
import { CategoryIcon } from "./CategoryIcon";
export function ItemCard({ item: i }: { item: Item }) {
  return (
    <Link to={"/items/" + i.id} className="item-card">
      <div className={"item-image " + i.type.toLowerCase()}>
        {i.imageUrl ? (
          <img src={i.imageUrl} alt={i.title} />
        ) : (
          <CategoryIcon name={i.category} size={64} />
        )}
        <span className={"badge " + i.type.toLowerCase()}>
          {i.type === "FOUND" ? "Found" : "Lost"}
        </span>
      </div>
      <div className="item-content">
        <div className="item-meta">
          {i.category}
          <span>{i.status.toLowerCase()}</span>
        </div>
        <h3>{i.title}</h3>
        <p>
          <MapPin size={14} />
          {i.location}
        </p>
        <div className="item-bottom">
          <span>
            <CalendarDays size={14} />
            {i.eventDate}
          </span>
          <ArrowUpRight size={19} />
        </div>
      </div>
    </Link>
  );
}
