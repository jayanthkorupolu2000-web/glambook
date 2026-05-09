import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';

@Component({
  selector: 'app-alert',
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.scss']
})
export class AlertComponent implements OnChanges, OnDestroy {
  @Input() message: string = '';
  @Input() type: 'success' | 'error' | 'info' | 'warning' = 'info';

  visible = false;
  private timer: ReturnType<typeof setTimeout> | null = null;

  get alertClass(): string {
    const map: Record<string, string> = {
      success: 'alert-success',
      error: 'alert-danger',
      info: 'alert-info',
      warning: 'alert-warning'
    };
    return map[this.type] ?? 'alert-info';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['message'] && this.message) {
      this.show();
    }
  }

  show(): void {
    this.visible = true;
    this.clearTimer();
    this.timer = setTimeout(() => this.dismiss(), 4000);
  }

  dismiss(): void {
    this.visible = false;
    this.clearTimer();
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }
}
