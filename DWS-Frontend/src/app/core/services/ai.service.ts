import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);
  private BASE = environment.apiUrl;

  getInsight(transactions: any[]): Observable<{ insight: string }> {
    return this.http.post<{ insight: string }>(
      `${this.BASE}/user/ai/insight`,
      { transactions }
    );
  }
}
