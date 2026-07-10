import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import {
  LucideArrowRight,
  LucideBriefcaseBusiness,
  LucideBuilding2,
  LucideCircleCheck,
  LucideCircleStop,
  LucideDownload,
  LucideExternalLink,
  LucideEye,
  LucideEyeOff,
  LucideFileSpreadsheet,
  LucideFileText,
  LucideKeyRound,
  LucideLayoutDashboard,
  LucideLink,
  LucideListChecks,
  LucideLocateFixed,
  LucideLock,
  LucideLockKeyholeOpen,
  LucideLogIn,
  LucideLogOut,
  LucideMapPin,
  LucideMegaphone,
  LucideMonitor,
  LucidePencil,
  LucidePlus,
  LucideRefreshCw,
  LucideSave,
  LucideSearch,
  LucideSend,
  LucideSettings,
  LucideTicket,
  LucideTrash2,
  LucideUnlink,
  LucideUpload,
  LucideUserPlus,
  LucideUsers,
  LucideX,
} from '@lucide/angular';

export type AppIconName =
  | 'arrow-right'
  | 'briefcase'
  | 'building'
  | 'check-circle'
  | 'circle-stop'
  | 'dashboard'
  | 'download'
  | 'external-link'
  | 'eye'
  | 'eye-off'
  | 'file-spreadsheet'
  | 'file-text'
  | 'key'
  | 'link'
  | 'list-checks'
  | 'locate'
  | 'lock'
  | 'login'
  | 'logout'
  | 'map-pin'
  | 'megaphone'
  | 'monitor'
  | 'pencil'
  | 'plus'
  | 'refresh'
  | 'save'
  | 'search'
  | 'send'
  | 'settings'
  | 'ticket'
  | 'trash'
  | 'unlink'
  | 'unlock'
  | 'upload'
  | 'user-plus'
  | 'users'
  | 'x';

@Component({
  selector: 'app-icon',
  standalone: true,
  host: { 'aria-hidden': 'true' },
  imports: [
    CommonModule,
    LucideArrowRight,
    LucideBriefcaseBusiness,
    LucideBuilding2,
    LucideCircleCheck,
    LucideCircleStop,
    LucideDownload,
    LucideExternalLink,
    LucideEye,
    LucideEyeOff,
    LucideFileSpreadsheet,
    LucideFileText,
    LucideKeyRound,
    LucideLayoutDashboard,
    LucideLink,
    LucideListChecks,
    LucideLocateFixed,
    LucideLock,
    LucideLockKeyholeOpen,
    LucideLogIn,
    LucideLogOut,
    LucideMapPin,
    LucideMegaphone,
    LucideMonitor,
    LucidePencil,
    LucidePlus,
    LucideRefreshCw,
    LucideSave,
    LucideSearch,
    LucideSend,
    LucideSettings,
    LucideTicket,
    LucideTrash2,
    LucideUnlink,
    LucideUpload,
    LucideUserPlus,
    LucideUsers,
    LucideX,
  ],
  templateUrl: './app-icon.html',
  styleUrl: './app-icon.scss',
})
export class AppIcon {
  @Input({ required: true }) name!: AppIconName;
  @Input() size = 18;
  @Input() strokeWidth = 2;
}
